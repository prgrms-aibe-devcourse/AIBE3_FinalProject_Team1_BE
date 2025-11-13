package com.back.domain.reservation.common;

import lombok.Getter;

@Getter
public enum ReservationStatus {
    PENDING_APPROVAL("승인 대기"),
    PENDING_PAYMENT("결제 대기"),
    PENDING_PICKUP("수령 대기"),
    SHIPPING("배송 중"),
    INSPECTING_RENTAL("대여 검수"),
    RENTING("대여 중"),
    PENDING_RETURN("반납 대기"),
    RETURNING("반납 중"),
    RETURN_COMPLETED("반납 완료"),
    INSPECTING_RETURN("반납 검수"),
    PENDING_REFUND("환급 예정"),
    REFUND_COMPLETED("환급 완료"),
    LOST_OR_UNRETURNED("미반납/분실"),
    CLAIMING("청구 진행"),
    CLAIM_COMPLETED("청구 완료"),
    REJECTED("승인 거절"),
    CANCELLED("예약 취소");

    private final String description;

    ReservationStatus(String description) {
        this.description = description;
    }

}
