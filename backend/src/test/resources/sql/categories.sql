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
INSERT INTO category (name, parent_id, created_at, modified_at)
VALUES
    ('등산/아웃도어', 1, NOW(), NOW()),
    ('구기종목', 1, NOW(), NOW()),
    ('골프/라켓', 1, NOW(), NOW()),

    ('청소/세탁', 2, NOW(), NOW()),
    ('주방/생활가전', 2, NOW(), NOW()),

    ('컴퓨터/노트북', 3, NOW(), NOW()),
    ('카메라/영상기기', 3, NOW(), NOW()),
    ('스마트폰/태블릿', 3, NOW(), NOW());
