SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE member;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO member (id, created_at, modified_at, address1, address2, email, is_banned, name, nickname, password, phone_number, profile_img_url, role)
VALUES
    (1, NOW(), NOW(), '서울특별시 강남구', '역삼동', 'user1@example.com', b'0', '김철수', 'chulsu', 'password1', '01012345678', NULL, 'USER'),
    (2, NOW(), NOW(), '경기도 성남시', '분당동', 'user2@example.com', b'0', '홍길동', 'hong', 'password2', '01023456789', NULL, 'USER'),
    (3, NOW(), NOW(), '서울특별시 서초구', '반포동', 'admin@example.com', b'0', '관리자', 'admin', 'adminpass', '01034567890', NULL, 'ADMIN');