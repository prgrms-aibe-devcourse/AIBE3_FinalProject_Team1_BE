USE chwimeet;

SET FOREIGN_KEY_CHECKS = 0;

-- 부하 테스트에 필요한 테이블만 정리
TRUNCATE TABLE category;
TRUNCATE TABLE member;
TRUNCATE TABLE region;
TRUNCATE TABLE post;
TRUNCATE TABLE post_image;
TRUNCATE TABLE post_option;
TRUNCATE TABLE post_region;
TRUNCATE TABLE post_favorite;
TRUNCATE TABLE reservation;
TRUNCATE TABLE review;
TRUNCATE TABLE chat_room;
TRUNCATE TABLE chat_member;
TRUNCATE TABLE chat_message;

SET FOREIGN_KEY_CHECKS = 1;


-- 카테고리
INSERT INTO category (id, name, parent_id, created_at, modified_at)
VALUES
    (1, '스포츠', NULL, NOW(), NOW()),
    (2, '가전', NULL, NOW(), NOW()),
    (3, '디지털', NULL, NOW(), NOW()),
    (4, '등산/아웃도어', 1, NOW(), NOW()),
    (5, '구기종목', 1, NOW(), NOW()),
    (6, '골프/라켓', 1, NOW(), NOW()),
    (7, '청소/세탁', 2, NOW(), NOW()),
    (8, '주방/생활가전', 2, NOW(), NOW()),
    (9, '컴퓨터/노트북', 3, NOW(), NOW()),
    (10, '카메라/영상기기', 3, NOW(), NOW()),
    (11, '스마트폰/태블릿', 3, NOW(), NOW());


-- 지역
INSERT INTO region (id, name, parent_id, created_at, modified_at)
VALUES
    (1, '서울특별시', NULL, NOW(), NOW()),
    (2, '경기도', NULL, NOW(), NOW()),
    (3, '강남구', 1, NOW(), NOW()),
    (4, '서초구', 1, NOW(), NOW()),
    (5, '성남시', 2, NOW(), NOW()),
    (6, '수원시', 2, NOW(), NOW());


-- 회원 100개
INSERT INTO member (
    email, password, name, phone_number, address1, address2,
    nickname, is_banned, role, profile_img_url,
    created_at, modified_at
)
WITH RECURSIVE seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n+1 FROM seq WHERE n < 100
)
SELECT
    CONCAT('loadtester', n, '@test.com'),
    '$2a$10$zUnDUq/JziYaF1uQ1eM.l.T1vzi0Ivw3wGaIbV.ShExutKsbUa5X6',
    CONCAT('사용자', n),
    CONCAT('010', LPAD(n, 4, '0'), LPAD(n, 4, '0')),
    '서울특별시 강남구',
    CONCAT('역삼동 ', n, '번지'),
    CONCAT('tester', n),
    FALSE,
    'USER',
    NULL,
    NOW(),
    NOW()
FROM seq;


-- 게시글 100개 (n번 회원이 n번 게시글 작성)
INSERT INTO post (
    title, content, receive_method, return_method,
    return_address1, return_address2,
    deposit, fee, is_banned,
    embedding_status, author_id, category_id,
    embedding_version, created_at, modified_at
)
WITH RECURSIVE seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n+1 FROM seq WHERE n < 100
)
SELECT
    CONCAT('테스트 게시글 ', n),
    CONCAT('내용: 이것은 테스트 게시글입니다. 번호=', n),
    'DIRECT',
    'DIRECT',
    '서울특별시 강남구',
    CONCAT('역삼동 ', n),
    10000 + (n * 10),
    5000 + (n * 5),
    FALSE,
    'DONE',
    n,
    (n MOD 11) + 1,
    1,
    NOW(),
    NOW()
FROM seq;


-- 게시글 옵션
INSERT INTO post_option (name, deposit, fee, post_id, created_at, modified_at)
SELECT
    CONCAT('옵션 ', id),
    0,
    0,
    id,
    NOW(),
    NOW()
FROM post;


-- 게시글 지역
INSERT INTO post_region (post_id, region_id, created_at, modified_at)
SELECT
    id,
    (id MOD 6) + 1,
    NOW(),
    NOW()
FROM post;


-- 즐겨찾기
INSERT INTO post_favorite (member_id, post_id, created_at, modified_at)
SELECT DISTINCT
    m.id,
    ((m.id + p.id - 2) MOD 100) + 1,
    NOW(),
    NOW()
FROM member m
         CROSS JOIN (
    SELECT id FROM post WHERE id <= 10
) p;


-- 예약 20개
INSERT INTO reservation (
    created_at, modified_at,
    receive_method, return_method,
    reservation_start_at, reservation_end_at,
    status, author_id, post_id
)
SELECT
    NOW(), NOW(),
    'DIRECT', 'DIRECT',
    NOW(), DATE_ADD(NOW(), INTERVAL 3 DAY),
    'PENDING_APPROVAL',
    id + 50,
    id
FROM post
WHERE id <= 20;


-- 리뷰 10개
INSERT INTO review (
    created_at, modified_at, comment, equipment_score,
    is_banned, kindness_score, response_time_score, reservation_id
)
SELECT
    NOW(), NOW(),
    CONCAT('리뷰 내용 ', id),
    5, 0, 5, 5,
    id
FROM reservation
WHERE id <= 10;


-- 채팅방 30개
INSERT INTO chat_room (post_id, post_title_snapshot, created_at, modified_at)
SELECT
    id,
    title,
    NOW(), NOW()
FROM post
WHERE id <= 30;


-- 채팅 멤버 (호스트 + 요청자, 총 60명)
-- 채팅방 1번: 회원 1번(호스트) + 회원 51번(요청자)
-- 채팅방 2번: 회원 2번(호스트) + 회원 52번(요청자)
INSERT INTO chat_member (
    chat_room_id, member_id, last_read_message_id, created_at, modified_at
)
SELECT
    r.id AS chat_room_id,
    p.author_id AS member_id,
    0,
    NOW(), NOW()
FROM chat_room r
         JOIN post p ON r.post_id = p.id

UNION ALL

SELECT
    r.id AS chat_room_id,
    p.author_id + 50 AS member_id,
    0,
    NOW(), NOW()
FROM chat_room r
         JOIN post p ON r.post_id = p.id;


-- 채팅 메시지 (각 멤버당 10개씩, 총 600개)
INSERT INTO chat_message (
    content, chat_room_id, chat_member_id, created_at, modified_at
)
WITH RECURSIVE msg_seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n+1 FROM msg_seq WHERE n < 10
)
SELECT
    CONCAT('메시지 ', msg_seq.n),
    cm.chat_room_id,
    cm.id,
    DATE_ADD(NOW(), INTERVAL -1 * (10 - msg_seq.n) MINUTE),
    DATE_ADD(NOW(), INTERVAL -1 * (10 - msg_seq.n) MINUTE)
FROM chat_member cm
         CROSS JOIN msg_seq
ORDER BY cm.chat_room_id, msg_seq.n;