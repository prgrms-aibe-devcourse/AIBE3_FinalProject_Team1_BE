SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE notification;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO notification (id, created_at, modified_at, is_read, target_id, type, member_id)
VALUES
    (1, NOW(), NOW(), b'0', 1, 'RESERVATION_RETURN_COMPLETED', 1),
    (2, NOW(), NOW(), b'0', 2, 'RESERVATION_PENDING_REFUND', 1),
    (3, NOW(), NOW(), b'1', 3, 'RESERVATION_CLAIM_COMPLETED', 1),
    (4, NOW(), NOW(), b'0', 1, 'REVIEW_CREATED', 1),
    (5, NOW(), NOW(), b'0', 2, 'REVIEW_CREATED', 1),
    (6, NOW(), NOW(), b'0', 3, 'REVIEW_CREATED', 1);
