SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE post;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO post (
    id, created_at, modified_at, content, deposit, fee,
    is_banned, receive_method, return_address1, return_address2,
    return_method, title, author_id, category_id,
    embedding_status, embedding_version
)
VALUES
    -- 멤버 1이 작성한 3개의 포스트
    (1, NOW(), NOW(), '자전거 빌려드립니다', 10000, 5000, b'0', 'DIRECT', '서울특별시 강남구', '역삼동', 'DIRECT', '자전거 대여', 1, 4, 'DONE', 1),
    (2, NOW(), NOW(), '등산 장비 대여합니다', 20000, 8000, b'0', 'DELIVERY', '서울특별시 강남구', '역삼동', 'DELIVERY', '등산 장비', 1, 5, 'PENDING', 1),
    (3, NOW(), NOW(), '골프채 대여 가능', 30000, 10000, b'0', 'DIRECT', '서울특별시 강남구', '역삼동', 'DIRECT', '골프채', 1, 6, 'WAIT', 1),

    -- 멤버 2가 작성한 3개의 포스트
    (4, NOW(), NOW(), '노트북 대여', 50000, 10000, b'0', 'DELIVERY', '경기도 성남시', '분당동', 'DELIVERY', '노트북 빌려요', 2, 7, 'PENDING', 1),
    (5, NOW(), NOW(), '카메라 대여', 40000, 8000, b'0', 'DIRECT', '경기도 성남시', '분당동', 'DIRECT', '카메라 빌려요', 2, 8, 'DONE', 1),
    (6, NOW(), NOW(), '스마트폰/태블릿 대여', 30000, 7000, b'0', 'DELIVERY', '경기도 성남시', '분당동', 'DELIVERY', '태블릿 빌려요', 2, 9, 'PENDING', 1);

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE post_region;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO post_region (id, created_at, modified_at, post_id, region_id)
VALUES
    -- 멤버 1 포스트
    (1, NOW(), NOW(), 1, 3),
    (2, NOW(), NOW(), 1, 4),
    (3, NOW(), NOW(), 2, 3),
    (4, NOW(), NOW(), 2, 4),
    (5, NOW(), NOW(), 3, 4),
    (6, NOW(), NOW(), 3, 3),

    -- 멤버 2 포스트
    (7, NOW(), NOW(), 4, 5),
    (8, NOW(), NOW(), 4, 6),
    (9, NOW(), NOW(), 5, 5),
    (10, NOW(), NOW(), 5, 6),
    (11, NOW(), NOW(), 6, 6),
    (12, NOW(), NOW(), 6, 5);
