package com.back.domain.notification.common;

public enum NotificationType {
    RESERVATION_PENDING_APPROVAL,      // 예약 : 승인 대기
    RESERVATION_PENDING_PAYMENT,       // 예약 : 결제 대기
    RESERVATION_PENDING_PICKUP,        // 예약 : 수령 대기
    RESERVATION_SHIPPING,              // 예약 : 배송 중
    RESERVATION_INSPECTING_RENTAL,     // 예약 : 대여 검수
    RESERVATION_RENTING,               // 예약 : 대여 중
    RESERVATION_PENDING_RETURN,        // 예약 : 반납 대기
    RESERVATION_RETURNING,             // 예약 : 반납 중
    RESERVATION_RETURN_COMPLETED,      // 예약 : 반납 완료
    RESERVATION_INSPECTING_RETURN,     // 예약 : 반납 검수
    RESERVATION_PENDING_REFUND,        // 예약 : 환급 예정
    RESERVATION_REFUND_COMPLETED,      // 예약 : 환급 완료
    RESERVATION_LOST_OR_UNRETURNED,    // 예약 : 미반납/분실
    RESERVATION_CLAIMING,              // 예약 : 청구 진행
    RESERVATION_CLAIM_COMPLETED,       // 예약 : 청구 완료
    RESERVATION_REJECTED,              // 예약 : 승인 거절
    RESERVATION_CANCELLED              // 예약 : 예약 취소
}
