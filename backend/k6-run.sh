#!/bin/bash

# 오류 발생 즉시 중단
set -e

# 실패 시 메시지 출력 함수
fail() {
  echo "❌ ERROR: $1"
  exit 1
}

# cleanup 함수
cleanup() {
  if [ "$SHOULD_CLEANUP" = true ]; then
    echo ""
    echo "====================================="
    echo "🧹 CLEANUP: 테스트 데이터 정리"
    echo "====================================="

    if [ -f "$SQL_CLEAN" ]; then
      docker exec -i chwimeet-mariadb \
          mariadb -u root -p"$SPRING__DATASOURCE__PASSWORD" chwimeet \
          < "$SQL_CLEAN" 2>/dev/null || echo "⚠️ cleanup.sql 실행 실패 (무시)"
      echo "➡ cleanup.sql 적용 완료"
    fi
  fi
}

# 스크립트 종료 시 cleanup 실행
trap cleanup EXIT

echo ""
echo "====================================="
echo "📌 STEP 0: 환경변수(.env) 로드"
echo "====================================="

if [ -f ".env" ]; then
  export $(grep -v '^#' .env | xargs) || fail ".env 로드 실패"
  echo "➡ .env 로드 완료"
else
  fail ".env 파일이 없습니다."
fi

# 스크립트가 위치한 디렉토리로 이동
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR" || fail "프로젝트 디렉토리로 이동 실패"

# 현재 스크립트 기준 경로 계산
ROOT_DIR="$SCRIPT_DIR"
K6_DIR="$ROOT_DIR/k6"
SQL_CLEAN="$K6_DIR/cleanup.sql"
SQL_INIT="$K6_DIR/init_data.sql"
SCRIPTS_DIR="$K6_DIR/scripts"

echo "ROOT_DIR = $ROOT_DIR"
echo "SQL_CLEAN = $SQL_CLEAN"
echo "SQL_INIT = $SQL_INIT"

echo ""
echo "====================================="
echo "📋 사용 가능한 테스트 스크립트"
echo "====================================="

# k6/scripts 폴더의 .js 파일 목록 출력
if [ -d "$SCRIPTS_DIR" ]; then
  ls -1 "$SCRIPTS_DIR"/*.js 2>/dev/null | while read -r file; do
    echo "  - $(basename "$file")"
  done
else
  fail "스크립트 디렉토리가 존재하지 않습니다: $SCRIPTS_DIR"
fi

echo ""
read -p "실행할 스크립트 파일명을 입력하세요 (예: load-test.js): " SCRIPT_NAME

# 입력값이 없으면 기본값 사용
if [ -z "$SCRIPT_NAME" ]; then
  SCRIPT_NAME="load-test.js"
  echo "➡ 기본값 사용: $SCRIPT_NAME"
fi

# 파일 존재 확인
if [ ! -f "$SCRIPTS_DIR/$SCRIPT_NAME" ]; then
  fail "스크립트 파일이 존재하지 않습니다: $SCRIPT_NAME"
fi

echo "➡ 선택된 스크립트: $SCRIPT_NAME"

echo ""
echo "====================================="
echo "⚙️ DB 초기화 설정"
echo "====================================="

read -p "DB를 초기화하고 테스트 데이터를 삽입하시겠습니까? (y/n, 기본값: y): " INIT_DB

# 입력값이 없거나 y/Y이면 초기화
if [ -z "$INIT_DB" ] || [ "$INIT_DB" = "y" ] || [ "$INIT_DB" = "Y" ]; then
  INIT_DB=true
  SHOULD_CLEANUP=true
  echo "➡ DB 초기화: 예"
else
  INIT_DB=false
  SHOULD_CLEANUP=false
  echo "➡ DB 초기화: 아니오 (기존 데이터 유지)"
fi

# DB 초기화
if [ "$INIT_DB" = true ]; then
  echo ""
  echo "====================================="
  echo "🚀 STEP 1: MariaDB 도커에서 cleanup.sql 실행"
  echo "====================================="

  docker exec -i chwimeet-mariadb \
      mariadb -u root -p"$SPRING__DATASOURCE__PASSWORD" chwimeet \
      < "$SQL_CLEAN" || fail "cleanup.sql 실행 실패"

  echo "➡ cleanup.sql 적용 완료"

  echo ""
  echo "====================================="
  echo "🚀 STEP 2: MariaDB 도커에 init_data.sql 삽입"
  echo "====================================="

  docker exec -i chwimeet-mariadb \
      mariadb -u root -p"$SPRING__DATASOURCE__PASSWORD" chwimeet \
      < "$SQL_INIT" || fail "init_data.sql 실행 실패"

  echo "➡ init_data.sql 적용 완료"
else
  echo ""
  echo "====================================="
  echo "⏭️ DB 초기화 건너뛰기"
  echo "====================================="
fi

echo ""
echo "====================================="
echo "🚀 STEP 3: $SCRIPT_NAME 실행"
echo "====================================="

MSYS_NO_PATHCONV=1 docker compose run --rm k6 \
    run --out "experimental-prometheus-rw=$K6_PROMETHEUS_RW_SERVER_URL" \
    /scripts/scripts/$SCRIPT_NAME || fail "$SCRIPT_NAME 실행 실패"

echo ""
echo "====================================="
echo "🎉 전체 테스트 완료!"
echo "====================================="