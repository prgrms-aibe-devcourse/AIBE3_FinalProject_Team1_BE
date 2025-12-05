SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE category;
SET FOREIGN_KEY_CHECKS = 1;

-- 부모 카테고리
INSERT INTO category (id, name, parent_id, created_at, modified_at)
VALUES
    (1, '스포츠', NULL, NOW(), NOW()),
    (2, '가전', NULL, NOW(), NOW()),
    (3, '디지털', NULL, NOW(), NOW());

-- 자식 카테고리
INSERT INTO category (id, name, parent_id, created_at, modified_at)
VALUES
    (4, '등산/아웃도어', 1, NOW(), NOW()),
    (5, '구기종목', 1, NOW(), NOW()),
    (6, '골프/라켓', 1, NOW(), NOW()),

    (7, '청소/세탁', 2, NOW(), NOW()),
    (8, '주방/생활가전', 2, NOW(), NOW()),

    (9, '컴퓨터/노트북', 3, NOW(), NOW()),
    (10, '카메라/영상기기', 3, NOW(), NOW()),
    (11, '스마트폰/태블릿', 3, NOW(), NOW());
