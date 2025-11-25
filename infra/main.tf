terraform {
  // aws 라이브러리 불러옴
  required_providers {
    aws = {
      source = "hashicorp/aws"
    }
  }
}

# AWS 설정 시작
provider "aws" {
  region = var.region
}
# AWS 설정 끝

# --- 1. 네트워크 (VPC, Subnet, IGW, Route) ---
resource "aws_vpc" "vpc-team1" {
  cidr_block = "10.0.0.0/16"

  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = {
    Name = "${var.prefix}-vpc" // team1-vpc
    Team = var.team_tag_value
  }
}

// AZ-a (Public Subnet for EC2)
resource "aws_subnet" "subnet-team1-a" {
  vpc_id                  = aws_vpc.vpc-team1.id
  cidr_block              = "10.0.0.0/24"
  availability_zone       = "${var.region}a"
  map_public_ip_on_launch = true

  tags = {
    Name = "${var.prefix}-subnet-a" // team1-subnet-a
    Team = var.team_tag_value
  }
}

// AZ-b (Public Subnet - 예비용 또는 확장 대비)
resource "aws_subnet" "subnet-team1-b" {
  vpc_id                  = aws_vpc.vpc-team1.id
  cidr_block              = "10.0.1.0/24"
  availability_zone       = "${var.region}b"
  map_public_ip_on_launch = true

  tags = {
    Name = "${var.prefix}-subnet-b" // team1-subnet-b
    Team = var.team_tag_value
  }
}

resource "aws_internet_gateway" "igw-team1" {
  vpc_id = aws_vpc.vpc-team1.id

  tags = {
    Name = "${var.prefix}-igw" // team1-igw
    Team = var.team_tag_value
  }
}

resource "aws_route_table" "rt-team1" {
  vpc_id = aws_vpc.vpc-team1.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.igw-team1.id
  }

  tags = {
    Name = "${var.prefix}-rt" // team1-rt
    Team = var.team_tag_value
  }
}

resource "aws_route_table_association" "assoc-a" {
  subnet_id      = aws_subnet.subnet-team1-a.id
  route_table_id = aws_route_table.rt-team1.id
}

resource "aws_route_table_association" "assoc-b" {
  subnet_id      = aws_subnet.subnet-team1-b.id
  route_table_id = aws_route_table.rt-team1.id
}

resource "aws_security_group" "sg-team1" {
  name = "${var.prefix}-sg" // team1-sg

  // 인바운드: 모든 IP (0.0.0.0/0)에서 모든 포트 허용 (보안 강화를 위해 최소 포트만 허용하도록 변경 권장)
  ingress {
    from_port = 0
    to_port   = 0
    protocol  = "all"
    cidr_blocks = ["0.0.0.0/0"]
  }

  // 아웃바운드: 모두 허용
  egress {
    from_port = 0
    to_port   = 0
    protocol  = "all"
    cidr_blocks = ["0.0.0.0/0"]
  }

  vpc_id = aws_vpc.vpc-team1.id

  tags = {
    Name = "${var.prefix}-sg" // team1-sg
    Team = var.team_tag_value
  }
}

# --- 2. IAM (EC2 Role for SSM & S3) ---
resource "aws_iam_role" "ec2-role-team1" {
  name = "${var.prefix}-ec2-role" // team1-ec2-role

  // EC2 서비스가 이 역할을 가정할 수 있도록 설정
  assume_role_policy = <<EOF
  {
    "Version": "2012-10-17",
    "Statement": [
      {
        "Sid": "",
        "Action": "sts:AssumeRole",
        "Principal": {
            "Service": "ec2.amazonaws.com"
        },
        "Effect": "Allow"
      }
    ]
  }
  EOF

  tags = {
    Name = "${var.prefix}-ec2-role"
    Team = var.team_tag_value
  }
}

// EC2 역할에 AmazonEC2RoleforSSM 정책을 부착 (SSM 통신을 위해 필수)
resource "aws_iam_role_policy_attachment" "ec2-ssm" {
  role       = aws_iam_role.ec2-role-team1.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonEC2RoleforSSM"
}

// S3 접근 권한 (필요에 따라 최소 권한으로 변경 권장)
resource "aws_iam_role_policy_attachment" "s3-full-access" {
  role       = aws_iam_role.ec2-role-team1.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonS3FullAccess"
}

// IAM 인스턴스 프로파일 생성
resource "aws_iam_instance_profile" "instance-profile-team1" {
  name = "${var.prefix}-instance-profile" // team1-instance-profile
  role = aws_iam_role.ec2-role-team1.name

  tags = {
    Name = "${var.prefix}-instance-profile"
    Team = var.team_tag_value
  }
}

# --- 3. EC2 인스턴스 (Blue/Green 배포 대상) ---
locals {
  ec2_user_data_base = <<-END_OF_FILE
#!/bin/bash
# ... (User Data Script 내용 동일하게 유지) ...
# 가상 메모리 4GB 설정
dd if=/dev/zero of=/swapfile bs=128M count=32
chmod 600 /swapfile
mkswap /swapfile
swapon /swapfile
sh -c 'echo "/swapfile swap swap defaults 0 0" >> /etc/fstab'

# 타임존 설정
timedatectl set-timezone Asia/Seoul

# 환경변수 세팅(/etc/environment)
echo "PASSWORD_1=${var.password_1}" >> /etc/environment
echo "APP_1_DOMAIN=${var.app_1_domain}" >> /etc/environment
echo "APP_1_DB_NAME=${var.app_1_db_name}" >> /etc/environment
echo "GITHUB_ACCESS_TOKEN_1_OWNER=${var.github_access_token_1_owner}" >> /etc/environment
echo "GITHUB_ACCESS_TOKEN_1=${var.github_access_token_1}" >> /etc/environment
echo "CUSTOM__JWT__SECRETKEY=${var.jwt_secret}" >> /etc/environment
echo "CUSTOM_CORS_ALLOWED_ORIGINS=${var.cors_allowed_origin}" >> /etc/environment
echo "OPENAI_API_KEY=${var.openai_api_key}" >> /etc/environment
echo "CLOUD__AWS__S3__BUCKET=${var.s3_bucket_name}" >> /etc/environment
echo "SPRING__MAIL__HOST=${var.mail_host}" >> /etc/environment
echo "SPRING__MAIL__PORT=${var.mail_port}" >> /etc/environment
echo "SPRING__MAIL__USERNAME=${var.mail_username}" >> /etc/environment
echo "SPRING__MAIL__PASSWORD=${var.mail_password}" >> /etc/environment
source /etc/environment

# 도커 설치 및 실행/활성화
yum install docker -y
systemctl enable docker
systemctl start docker

# Docker Compose 설치
curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose
ln -s /usr/local/bin/docker-compose /usr/bin/docker-compose

# 도커 네트워크 생성
docker network create common

# nginx proxy manager 설치 (npm_1 컨테이너 이름 유지)
docker run -d \
  --name npm_1 \
  --restart unless-stopped \
  --network common \
  -p 80:80 \
  -p 443:443 \
  -p 81:81 \
  -e TZ=Asia/Seoul \
  -e INITIAL_ADMIN_EMAIL=admin@example.com \
  -e INITIAL_ADMIN_PASSWORD=${var.password_1} \
  -v /dockerProjects/npm_1/volumes/data:/data \
  -v /dockerProjects/npm_1/volumes/etc/letsencrypt:/etc/letsencrypt \
  jc21/nginx-proxy-manager:latest

# redis 설치 (redis_1 컨테이너 이름 유지)
docker run -d \
  --name=redis_1 \
  --restart unless-stopped \
  --network common \
  -p 6379:6379 \
  -e TZ=Asia/Seoul \
  -v /dockerProjects/redis_1/volumes/data:/data \
  redis --requirepass ${var.password_1}

# mysql 설치 (mysql_1 컨테이너 이름 유지)
docker run -d \
  --name mysql_1 \
  --restart unless-stopped \
  -v /dockerProjects/mysql_1/volumes/var/lib/mysql:/var/lib/mysql \
  -v /dockerProjects/mysql_1/volumes/etc/mysql/conf.d:/etc/mysql/conf.d \
  --network common \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=${var.password_1} \
  -e TZ=Asia/Seoul \
  mysql:latest

# MySQL 컨테이너가 준비될 때까지 대기
echo "MySQL이 기동될 때까지 대기 중..."
until docker exec mysql_1 mysql -uroot -p${var.password_1} -e "SELECT 1" &> /dev/null; do
  echo "MySQL이 아직 준비되지 않음. 5초 후 재시도..."
  sleep 5
done
echo "MySQL이 준비됨. 초기화 스크립트 실행 중..."

# MySQL 데이터베이스 생성
docker exec mysql_1 mysql -uroot -p${var.password_1} -e "
CREATE DATABASE IF NOT EXISTS \`${var.app_1_db_name}\`;
FLUSH PRIVILEGES;
"

# GitHub Container Registry 로그인
echo "${var.github_access_token_1}" |
docker login ghcr.io -u ${var.github_access_token_1_owner} --password-stdin

# 애플리케이션 디렉토리 생성
mkdir -p /home/ec2-user/app
cd /home/ec2-user/app

# .env 파일 생성
cat > .env <<'ENV_EOF'
SPRING__DATASOURCE__URL=jdbc:mysql://mysql_1:3306/${var.app_1_db_name}?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
SPRING__DATASOURCE__USERNAME=root
SPRING__DATASOURCE__PASSWORD=${var.password_1}
SPRING__REDIS__HOST=redis_1
SPRING__REDIS__PORT=6379
SPRING__REDIS__PASSWORD=${var.password_1}
CUSTOM__JWT__SECRETKEY=${var.jwt_secret}
CUSTOM_CORS_ALLOWED_ORIGINS=${var.cors_allowed_origin}
OPENAI_API_KEY=${var.openai_api_key}
CLOUD__AWS__S3__BUCKET=${var.s3_bucket_name}
SPRING__MAIL__HOST=${var.mail_host}
SPRING__MAIL__PORT=${var.mail_port}
SPRING__MAIL__USERNAME=${var.mail_username}
SPRING__MAIL__PASSWORD=${var.mail_password}
ENV_EOF

# docker-compose.yml 생성 (컨테이너 이름은 team1-app-001/002 유지)
cat > docker-compose.yml <<'COMPOSE_EOF'
version: '3.8'

services:
  team1-app-001:
    image: ghcr.io/${var.github_access_token_1_owner}/chwimeet-backend:latest
    container_name: team1-app-001
    restart: unless-stopped
    networks:
      - common
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SPRING_DATASOURCE_URL=$${SPRING__DATASOURCE__URL}
      - SPRING_DATASOURCE_USERNAME=$${SPRING__DATASOURCE__USERNAME}
      - SPRING_DATASOURCE_PASSWORD=$${SPRING__DATASOURCE__PASSWORD}
      - SPRING_DATA_REDIS_HOST=$${SPRING__REDIS__HOST}
      - SPRING_DATA_REDIS_PORT=$${SPRING__REDIS__PORT}
      - SPRING_DATA_REDIS_PASSWORD=$${SPRING__REDIS__PASSWORD}
      - CUSTOM__JWT__SECRETKEY=$${CUSTOM__JWT__SECRETKEY}
      - CUSTOM_CORS_ALLOWED_ORIGINS=$${CUSTOM_CORS_ALLOWED_ORIGINS}
      - OPENAI_API_KEY=$${OPENAI_API_KEY}
      - CLOUD__AWS__S3__BUCKET=$${CLOUD__AWS__S3__BUCKET}
      - SPRING__MAIL__HOST=$${SPRING__MAIL__HOST}
      - SPRING__MAIL__PORT=$${SPRING__MAIL__PORT}
      - SPRING__MAIL__USERNAME=$${SPRING__MAIL__USERNAME}
      - SPRING__MAIL__PASSWORD=$${SPRING__MAIL__PASSWORD}

    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

  team1-app-002:
    image: ghcr.io/${var.github_access_token_1_owner}/chwimeet-backend:latest
    container_name: team1-app-002
    restart: unless-stopped
    networks:
      - common
    ports:
      - "8081:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SPRING_DATASOURCE_URL=$${SPRING__DATASOURCE__URL}
      - SPRING_DATASOURCE_USERNAME=$${SPRING__DATASOURCE__USERNAME}
      - SPRING_DATASOURCE_PASSWORD=$${SPRING__DATASOURCE__PASSWORD}
      - SPRING_DATA_REDIS_HOST=$${SPRING__REDIS__HOST}
      - SPRING_DATA_REDIS_PORT=$${SPRING__REDIS__PORT}
      - SPRING_DATA_REDIS_PASSWORD=$${SPRING__REDIS__PASSWORD}
      - CUSTOM__JWT__SECRETKEY=$${CUSTOM__JWT__SECRETKEY}
      - CUSTOM_CORS_ALLOWED_ORIGINS=$${CUSTOM_CORS_ALLOWED_ORIGINS}
      - OPENAI_API_KEY=$${OPENAI_API_KEY}
      - CLOUD__AWS__S3__BUCKET=$${CLOUD__AWS__S3__BUCKET}
      - SPRING__MAIL__HOST=$${SPRING__MAIL__HOST}
      - SPRING__MAIL__PORT=$${SPRING__MAIL__PORT}
      - SPRING__MAIL__USERNAME=$${SPRING__MAIL__USERNAME}
      - SPRING__MAIL__PASSWORD=$${SPRING__MAIL__PASSWORD}
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    profiles:
      - blue-green

networks:
  common:
    external: true
COMPOSE_EOF

# 무중단 배포 스크립트 생성 (deploy.sh) - 완전 자동화 버전
cat > /home/ec2-user/app/deploy.sh <<'DEPLOY_EOF'
#!/bin/bash
set -e

echo "=========================================="
echo "Starting Blue-Green Deployment..."
echo "=========================================="

cd /home/ec2-user/app
source /etc/environment

# GitHub Container Registry 로그인
echo "$GITHUB_ACCESS_TOKEN_1" | docker login ghcr.io -u $GITHUB_ACCESS_TOKEN_1_OWNER --password-stdin

# 최신 이미지 Pull
echo "Pulling latest image..."
docker rmi ghcr.io/$GITHUB_ACCESS_TOKEN_1_OWNER/chwimeet-backend:latest || true
docker pull ghcr.io/$GITHUB_ACCESS_TOKEN_1_OWNER/chwimeet-backend:latest

# 현재 실행 중인 컨테이너 확인
if docker ps | grep -q team1-app-001; then
  CURRENT_CONTAINER="team1-app-001"
  NEW_CONTAINER="team1-app-002"
  CURRENT_PORT=8080
  NEW_PORT=8081
else
  CURRENT_CONTAINER="team1-app-002"
  NEW_CONTAINER="team1-app-001"
  CURRENT_PORT=8081
  NEW_PORT=8080
fi

echo "Current: $CURRENT_CONTAINER (port $CURRENT_PORT)"
echo "New: $NEW_CONTAINER (port $NEW_PORT)"

# 새 컨테이너 시작
echo "Starting new container: $NEW_CONTAINER..."
if [ "$NEW_CONTAINER" = "team1-app-002" ]; then
  docker-compose --profile blue-green up -d $NEW_CONTAINER
else
  docker-compose up -d $NEW_CONTAINER
fi

# Health check
echo "Running health checks..."
MAX_RETRIES=30
RETRY_COUNT=0

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
  if curl -f http://localhost:$NEW_PORT/actuator/health > /dev/null 2>&1; then
    echo "Health check passed!"
    break
  fi
  echo "Waiting for health check... ($((RETRY_COUNT+1))/$MAX_RETRIES)"
  sleep 5
  RETRY_COUNT=$((RETRY_COUNT+1))
done

if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
  echo "Health check failed! Rolling back..."
  docker-compose stop $NEW_CONTAINER
  docker-compose rm -f $NEW_CONTAINER
  exit 1
fi

# Nginx 자동 전환 (docker exec로 직접 업데이트)
echo "Switching Nginx to new container..."

# Nginx Proxy Manager 컨테이너에서 직접 DB 업데이트
docker exec npm_1 sqlite3 /data/database.sqlite \
  "UPDATE proxy_host SET forward_host = '$NEW_CONTAINER' WHERE domain_names LIKE '%${var.app_1_domain}%';" || {
  echo "Warning: Nginx auto-switch failed. Manual switch required."
}

# Nginx 리로드
docker exec npm_1 nginx -s reload || true

# 구 컨테이너 정리 (30초 대기 후)
echo "Waiting 30 seconds before removing old container..."
sleep 30

echo "Removing old container: $CURRENT_CONTAINER"
docker-compose stop $CURRENT_CONTAINER || true
docker-compose rm -f $CURRENT_CONTAINER || true

echo "=========================================="
echo "Deployment Completed!"
echo "Active container: $NEW_CONTAINER"
echo "=========================================="

DEPLOY_EOF

chmod +x /home/ec2-user/app/deploy.sh

# 초기 배포
cd /home/ec2-user/app
docker-compose up -d team1-app-001

# 헬스체크 대기
echo "Waiting for application to start..."
sleep 60

# 초기화 완료 메시지
PUBLIC_IP=$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4)
echo "=========================================="
echo "✅ EC2 initialization completed!"
echo "=========================================="
echo ""
echo "📋 Access Information:"
echo "Nginx Proxy Manager: http://$PUBLIC_IP:81"
echo "  - Email: admin@example.com"
echo "  - Password: ${var.password_1}"
echo ""
echo "=========================================="
echo "📖 Next Steps:"
echo "1. Login to Nginx Proxy Manager (http://$PUBLIC_IP:81)"
echo "2. Add Proxy Host:"
echo "   - Domain: ${var.app_1_domain}"
// 컨테이너 이름 team1-app-001
echo "   - Forward Hostname/IP: team1-app-001"
echo "   - Forward Port: 8080"
echo "3. Add SSL Certificate (Let's Encrypt)"
echo "4. Test: https://${var.app_1_domain}"
echo "=========================================="

END_OF_FILE
}

// 최신 Amazon Linux 2023 AMI 조회
data "aws_ami" "latest-amazon-linux" {
  most_recent = true
  owners = ["amazon"]

  filter {
    name = "name"
    values = ["al2023-ami-2023.*-x86_64"]
  }

  filter {
    name = "architecture"
    values = ["x86_64"]
  }

  filter {
    name = "virtualization-type"
    values = ["hvm"]
  }

  filter {
    name = "root-device-type"
    values = ["ebs"]
  }
}

// Elastic IP 생성
resource "aws_eip" "eip-team1" {
  domain = "vpc"

  tags = {
    Name = "${var.prefix}-eip" // team1-eip
    Team = var.team_tag_value
  }
}

// EC2 인스턴스 생성
resource "aws_instance" "ec2-team1" {
  ami           = data.aws_ami.latest-amazon-linux.id
  instance_type = "t3.small"
  key_name = "team1-key"
  subnet_id     = aws_subnet.subnet-team1-a.id
  vpc_security_group_ids = [aws_security_group.sg-team1.id]
  associate_public_ip_address = true
  iam_instance_profile = aws_iam_instance_profile.instance-profile-team1.name

  tags = {
    Name = "${var.prefix}-backend" // team1-backend (배포 대상 EC2 태그)
    Team = var.team_tag_value
  }

  root_block_device {
    volume_type = "gp3"
    volume_size = 30
  }

  user_data = <<-EOF
${local.ec2_user_data_base}
EOF
}

// Elastic IP를 EC2 인스턴스에 연결
resource "aws_eip_association" "eip-assoc-team1" {
  instance_id   = aws_instance.ec2-team1.id
  allocation_id = aws_eip.eip-team1.id
}

# --- 4. S3 Bucket (image uploads) ---
resource "aws_s3_bucket" "app_bucket" {
  bucket = var.s3_bucket_name

  tags = {
    Name = "${var.prefix}-s3-bucket"
    Team = var.team_tag_value
  }
}
