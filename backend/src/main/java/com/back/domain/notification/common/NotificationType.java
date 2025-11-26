package com.back.domain.notification.common;

import com.back.domain.reservation.common.ReservationStatus;

public enum NotificationType {

    // 상태 변경 알림
    RESERVATION_PENDING_APPROVAL(GroupType.RESERVATION),      // 예약 : 승인 대기
    RESERVATION_PENDING_PAYMENT(GroupType.RESERVATION),       // 예약 : 결제 대기
    RESERVATION_PENDING_PICKUP(GroupType.RESERVATION),        // 예약 : 수령 대기
    RESERVATION_SHIPPING(GroupType.RESERVATION),              // 예약 : 배송 중
    RESERVATION_INSPECTING_RENTAL(GroupType.RESERVATION),     // 예약 : 대여 검수
    RESERVATION_RENTING(GroupType.RESERVATION),               // 예약 : 대여 중
    RESERVATION_PENDING_RETURN(GroupType.RESERVATION),        // 예약 : 반납 대기
    RESERVATION_RETURNING(GroupType.RESERVATION),             // 예약 : 반납 중
    RESERVATION_RETURN_COMPLETED(GroupType.RESERVATION),      // 예약 : 반납 완료
    RESERVATION_INSPECTING_RETURN(GroupType.RESERVATION),     // 예약 : 반납 검수
    RESERVATION_PENDING_REFUND(GroupType.RESERVATION),        // 예약 : 환급 예정
    RESERVATION_REFUND_COMPLETED(GroupType.RESERVATION),      // 예약 : 환급 완료
    RESERVATION_LOST_OR_UNRETURNED(GroupType.RESERVATION),    // 예약 : 미반납/분실
    RESERVATION_CLAIMING(GroupType.RESERVATION),              // 예약 : 청구 진행
    RESERVATION_CLAIM_COMPLETED(GroupType.RESERVATION),       // 예약 : 청구 완료
    RESERVATION_REJECTED(GroupType.RESERVATION),              // 예약 : 승인 거절
    RESERVATION_CANCELLED(GroupType.RESERVATION),             // 예약 : 예약 취소

    // 리마인드 알림
    REMIND_RETURN_DUE(GroupType.RESERVATION);

    private final GroupType groupType;

    NotificationType(GroupType groupType) {
        this.groupType = groupType;
    }

    public GroupType getGroupType() {
        return groupType;
    }

    public static NotificationType reservationStatusToNotificationType(ReservationStatus status) {
        String typeName = "RESERVATION_" + status.name();
        return NotificationType.valueOf(typeName);
    }

    public enum GroupType {
        RESERVATION,
    }
}
