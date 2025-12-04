SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE chat_message;
TRUNCATE TABLE chat_member;
TRUNCATE TABLE chat_room;
TRUNCATE TABLE post;
TRUNCATE TABLE category;
TRUNCATE TABLE member;

SET FOREIGN_KEY_CHECKS = 1;

-- MEMBER --------------------------------------------------------
INSERT INTO member (email, password, name, phone_number, address1, address2, nickname, is_banned, role, profile_img_url, created_at, modified_at)
VALUES
    ('user1@test.com', '{noop}1234', '홍길동', '010-1111-1111', '서울시 강남구', '테헤란로 123', 'hong', false, 'USER', NULL, NOW(), NOW()),
    ('user2@test.com', '{noop}1234', '김철수', '010-2222-2222', '서울시 서초구', '서초대로 456', 'kim', false, 'USER', NULL, NOW(), NOW()),
    ('user3@test.com', '{noop}1234', '이영희', '010-3333-3333', '서울시 마포구', '월드컵북로 789', 'lee', false, 'USER', NULL, NOW(), NOW()),
    ('user4@test.com', '{noop}1234', '박민수', '010-4444-4444', '서울시 송파구', '잠실로 101', 'park', false, 'USER', NULL, NOW(), NOW());

-- CATEGORY --------------------------------------------------------
INSERT INTO category (name, parent_id, created_at, modified_at)
VALUES ('노트북', NULL, NOW(), NOW());

-- POST -------------------------------------------------------------
INSERT INTO post (title, content, receive_method, return_method,
                  return_address1, return_address2, deposit, fee,
                  is_banned, embedding_status, author_id, category_id, embedding_version,
                  created_at, modified_at)
VALUES
    ('캠핑 텐트 대여', '4인용 텐트입니다.', 'DELIVERY', 'DELIVERY',
     NULL, NULL, 10000, 5000, false, 'WAIT', 2, 1, 0, NOW(), NOW()),

    ('노트북 대여합니다', '맥북 프로입니다.', 'DELIVERY', 'DELIVERY',
     NULL, NULL, 50000, 20000, false, 'WAIT', 3, 1, 0, NOW(), NOW()),

    ('카메라 렌탈', '소니 A7III입니다.', 'DELIVERY', 'DELIVERY',
     NULL, NULL, 30000, 15000, false, 'WAIT', 4, 1, 0, NOW(), NOW()),

    ('확장 배터리 대여합니다', '보조 배터리 20000mAh', 'DELIVERY', 'DELIVERY',
     NULL, NULL, 8000, 3000, false, 'WAIT', 3, 1, 0, NOW(), NOW()),

    ('사용자1의 테스트 게시글', '테스트용 게시글입니다.', 'DELIVERY', 'DELIVERY',
     NULL, NULL, 0, 0, false, 'WAIT', 1, 1, 0, NOW(), NOW());

-- CHAT_ROOM --------------------------------------------------------
INSERT INTO chat_room (post_id, post_title_snapshot, last_message, last_message_time, created_at, modified_at)
VALUES
    (1, '캠핑 텐트 대여', NULL, NULL, NOW(), NOW()),
    (2, '노트북 대여합니다', NULL, NULL, NOW(), NOW()),
    (3, '카메라 렌탈', NULL, NULL, NOW(), NOW());

-- CHAT_MEMBER ------------------------------------------------------
-- ChatRoom 1 (Post author: user2, Guest: user1)
INSERT INTO chat_member (chat_room_id, member_id, last_read_message_id, created_at, modified_at)
VALUES
    (1, 2, 0, NOW(), NOW()),  -- host
    (1, 1, 0, NOW(), NOW());  -- guest

-- ChatRoom 2 (Post author: user3, Guest: user1)
INSERT INTO chat_member (chat_room_id, member_id, last_read_message_id, created_at, modified_at)
VALUES
    (2, 3, 0, NOW(), NOW()),  -- host
    (2, 1, 0, NOW(), NOW());  -- guest

-- ChatRoom 3 (Post author: user4, Guest: user1)
INSERT INTO chat_member (chat_room_id, member_id, last_read_message_id, created_at, modified_at)
VALUES
    (3, 4, 0, NOW(), NOW()),  -- host
    (3, 1, 0, NOW(), NOW());  -- guest

-- CHAT_MESSAGE ---------------------------------------------------------
INSERT INTO chat_message (content, chat_room_id, chat_member_id, created_at, modified_at)
VALUES
    ('메시지 1', 1, 2, NOW(), NOW()),
    ('메시지 2', 1, 2, NOW(), NOW()),
    ('메시지 3', 1, 2, NOW(), NOW());
