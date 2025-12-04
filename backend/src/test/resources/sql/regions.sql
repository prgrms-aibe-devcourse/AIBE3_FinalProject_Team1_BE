SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE region;
SET FOREIGN_KEY_CHECKS = 1;

-- 부모 지역 삽입
INSERT INTO region (id, name, parent_id, created_at, modified_at)
VALUES
    (1, '서울특별시', NULL, NOW(), NOW()),
    (2, '경기도', NULL, NOW(), NOW());

-- 자식 지역 삽입
INSERT INTO region (id, name, parent_id, created_at, modified_at)
VALUES
    (3, '강남구', 1, NOW(), NOW()),
    (4, '서초구', 1, NOW(), NOW()),
    (5, '성남시', 2, NOW(), NOW()),
    (6, '수원시', 2, NOW(), NOW());

