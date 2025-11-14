package com.back.domain.reservation.common;

import lombok.Getter;

@Getter
public enum ReservationStatus {
    PENDING_APPROVAL("승인 대기", false, ReservationStatusSubject.SYSTEM_OR_ANY),
    PENDING_PAYMENT("결제 대기", false, ReservationStatusSubject.HOST),
    PENDING_PICKUP("수령 대기", false, ReservationStatusSubject.SYSTEM_OR_ANY),
    SHIPPING("배송 중", false, ReservationStatusSubject.HOST),
    INSPECTING_RENTAL("대여 검수", false, ReservationStatusSubject.SYSTEM_OR_ANY),
    RENTING("대여 중", false, ReservationStatusSubject.GUEST),
    PENDING_RETURN("반납 대기", false, ReservationStatusSubject.SYSTEM_OR_ANY),
    RETURNING("반납 중", false, ReservationStatusSubject.GUEST),
    RETURN_COMPLETED("반납 완료", true, ReservationStatusSubject.SYSTEM_OR_ANY),
    INSPECTING_RETURN("반납 검수", true, ReservationStatusSubject.HOST),
    PENDING_REFUND("환급 예정", true, ReservationStatusSubject.HOST),
    REFUND_COMPLETED("환급 완료", true, ReservationStatusSubject.SYSTEM_OR_ANY),
    LOST_OR_UNRETURNED("미반납/분실", false, ReservationStatusSubject.HOST),
    CLAIMING("청구 진행", false, ReservationStatusSubject.HOST),
    CLAIM_COMPLETED("청구 완료", true, ReservationStatusSubject.SYSTEM_OR_ANY),
    REJECTED("승인 거절", false, ReservationStatusSubject.HOST),
    CANCELLED("예약 취소", false, ReservationStatusSubject.GUEST);

    private final String description;
    private final boolean isReviewable;
    private final ReservationStatusSubject statusSubject;

    ReservationStatus(String description, boolean isReviewable, ReservationStatusSubject statusSubject) {
        this.description = description;
        this.isReviewable = isReviewable;
        this.statusSubject = statusSubject;
    }
}