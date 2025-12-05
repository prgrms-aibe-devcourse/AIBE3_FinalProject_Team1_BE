SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE review;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO review (id, created_at, modified_at, comment, equipment_score, is_banned, kindness_score, response_time_score, reservation_id)
VALUES
    -- 멤버 1 예약 (reservation_id 1,2,3)
    (1, NOW(), NOW(), '장비 상태가 좋습니다', 5, b'0', 5, 5, 1),
    (2, NOW(), NOW(), '빠른 응답 감사합니다', 4, b'0', 5, 4, 2),
    (3, NOW(), NOW(), '청구 처리가 깔끔했습니다', 5, b'0', 4, 5, 3),

    -- 멤버 2 예약 (reservation_id 4,5,6)
    (4, NOW(), NOW(), '장비 상태 양호', 5, b'0', 5, 5, 4),
    (5, NOW(), NOW(), '응답이 빠릅니다', 4, b'0', 5, 4, 5),
    (6, NOW(), NOW(), '청구 완료 후 확인까지 완벽', 5, b'0', 5, 5, 6);