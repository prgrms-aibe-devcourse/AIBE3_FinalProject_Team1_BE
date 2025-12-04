SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE review;
TRUNCATE TABLE reservation;
TRUNCATE TABLE post;
TRUNCATE TABLE category;
TRUNCATE TABLE member;

SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO member (id, created_at, modified_at, address1, address2, email, is_banned, name, nickname, password, phone_number, profile_img_url, role)
VALUES (1, now(), now(), '', '', 'test-1@email.com', 0, 'Josephine Chand',
        'Mariya Cheng', 'B3Lc1NQtw', '01011112222', 'https://example-1.com', 'USER'),
       (2, now(), now(), '', '', 'test-2@email.com', 0, 'Xing John',
        'Raj Zhuang', 'YP7Jm2SvCt', '01022223333', 'https://example-2.com', 'USER');

INSERT INTO category (id, created_at, modified_at, name, parent_id)
VALUES (1, now(), now(), 'IT', null),
       (2, now(), now(), '스포츠', null);

INSERT INTO post (id, author_id, category_id, created_at, modified_at, content, deposit, fee, is_banned, receive_method, return_address1, return_address2, return_method, title, embedding_status, embedding_version)
VALUES (1, 1, 1, now(), now(), 'content-1', 1000, 100, 0,
        'DIRECT', '176.125.207.196', '193.51.60.132', 'DIRECT', 'title', 'DONE', '0'),
       (2, 2, 2, now(), now(), 'content-2', 2000, 200, 0,
        'DIRECT', '176.125.207.196', '193.51.60.132', 'DIRECT', 'title', 'DONE', '0');

INSERT INTO reservation (id, author_id, post_id, created_at, modified_at, cancel_reason, claim_reason, receive_address1, receive_address2, receive_carrier, receive_method, receive_tracking_number, reject_reason, reservation_end_at, reservation_start_at, return_carrier, return_method, return_tracking_number, status)
VALUES (1, 2, 2, now(), now(), '', '', '239.222.101.166', '216.149.58.145','',
        'DIRECT', '', '', now(), now(), '', 'DIRECT','', 'RENTING');

INSERT INTO review (id, created_at, modified_at, comment, equipment_score, is_banned, kindness_score, response_time_score, reservation_id)
VALUES (1, now(), now(), 'comment', 3, 0, 3, 3, 1);
