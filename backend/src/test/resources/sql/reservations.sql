SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE reservation;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO reservation (id, created_at, modified_at, cancel_reason, claim_reason, receive_address1, receive_address2, receive_carrier, receive_method, receive_tracking_number, reject_reason, reservation_end_at, reservation_start_at, return_carrier, return_method, return_tracking_number, status, author_id, post_id)
VALUES
    -- 멤버 1이 예약한 멤버 2 Post (Post ID: 4,5,6)
    (1, NOW(), NOW(), NULL, NULL, '서울특별시 강남구', '역삼동', 'CJ대한통운', 'DELIVERY', 'TRK100001', NULL, NOW() + INTERVAL 3 DAY, NOW(), 'CJ대한통운', 'DELIVERY', 'TRK100002', 'RETURN_COMPLETED', 1, 4),
    (2, NOW(), NOW(), NULL, NULL, '서울특별시 강남구', '역삼동', '한진택배', 'DELIVERY', 'TRK100003', NULL, NOW() + INTERVAL 4 DAY, NOW(), '한진택배', 'DELIVERY', 'TRK100004', 'PENDING_REFUND', 1, 5),
    (3, NOW(), NOW(), NULL, NULL, '서울특별시 강남구', '역삼동', 'CJ대한통운', 'DELIVERY', 'TRK100005', NULL, NOW() + INTERVAL 5 DAY, NOW(), 'CJ대한통운', 'DELIVERY', 'TRK100006', 'CLAIM_COMPLETED', 1, 6),

    -- 멤버 2가 예약한 멤버 1 Post (Post ID: 1,2,3)
    (4, NOW(), NOW(), NULL, NULL, '경기도 성남시', '분당동', 'CJ대한통운', 'DELIVERY', 'TRK200001', NULL, NOW() + INTERVAL 3 DAY, NOW(), 'CJ대한통운', 'DELIVERY', 'TRK200002', 'RETURN_COMPLETED', 2, 1),
    (5, NOW(), NOW(), NULL, NULL, '경기도 성남시', '분당동', '한진택배', 'DELIVERY', 'TRK200003', NULL, NOW() + INTERVAL 4 DAY, NOW(), '한진택배', 'DELIVERY', 'TRK200004', 'PENDING_REFUND', 2, 2),
    (6, NOW(), NOW(), NULL, NULL, '경기도 성남시', '분당동', 'CJ대한통운', 'DELIVERY', 'TRK200005', NULL, NOW() + INTERVAL 5 DAY, NOW(), 'CJ대한통운', 'DELIVERY', 'TRK200006', 'CLAIM_COMPLETED', 2, 3),

    -- 추가 예약 데이터 (멤버 2가 멤버 1의 Post 하나 예약)
    (7, NOW(), NOW(), NULL, NULL, NULL, NULL, NULL, 'DIRECT', NULL, NULL, NOW() + INTERVAL 15 DAY, NOW() + INTERVAL 7 DAY, NULL, 'DIRECT', NULL, 'PENDING_APPROVAL', 2, 1);