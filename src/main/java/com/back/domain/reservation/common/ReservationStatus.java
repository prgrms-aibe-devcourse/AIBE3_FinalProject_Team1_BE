package com.back.domain.reservation.common;

import lombok.Getter;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

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
    INSPECTING_RETURN("반납 검수", false, ReservationStatusSubject.HOST),
    RETURN_COMPLETED("반납 완료", true, ReservationStatusSubject.SYSTEM_OR_ANY),
    PENDING_REFUND("환급 예정", true, ReservationStatusSubject.HOST),
    REFUND_COMPLETED("환급 완료", true, ReservationStatusSubject.SYSTEM_OR_ANY),
    LOST_OR_UNRETURNED("미반납/분실", false, ReservationStatusSubject.SYSTEM_OR_ANY),
    CLAIMING("청구 진행", true, ReservationStatusSubject.HOST),
    CLAIM_COMPLETED("청구 완료", true, ReservationStatusSubject.SYSTEM_OR_ANY),
    REJECTED("승인 거절", false, ReservationStatusSubject.HOST),
    CANCELLED("예약 취소", false, ReservationStatusSubject.GUEST);

    private final String description;
    private final boolean isReviewable;
    private final ReservationStatusSubject statusSubject;

    // 정적 Map으로 전환 가능한 상태 관리
    private static final Map<ReservationStatus, Set<ReservationStatus>> ALLOWED_TRANSITIONS;

    static {
        Map<ReservationStatus, Set<ReservationStatus>> transitions = new EnumMap<>(ReservationStatus.class);

        // 각 상태별 전환 가능한 상태 정의
        transitions.put(PENDING_APPROVAL, Set.of(PENDING_PAYMENT, REJECTED, CANCELLED));
        transitions.put(PENDING_PAYMENT, Set.of(PENDING_PICKUP, CANCELLED));
        transitions.put(PENDING_PICKUP, Set.of(SHIPPING, INSPECTING_RENTAL, CANCELLED));
        transitions.put(SHIPPING, Set.of(INSPECTING_RENTAL));
        transitions.put(INSPECTING_RENTAL, Set.of(RENTING, PENDING_RETURN)); // 취소 → 반납 대기
        transitions.put(RENTING, Set.of(PENDING_RETURN, LOST_OR_UNRETURNED));
        transitions.put(PENDING_RETURN, Set.of(RETURNING, INSPECTING_RETURN));
        transitions.put(RETURNING, Set.of(INSPECTING_RETURN));
        transitions.put(INSPECTING_RETURN, Set.of(RETURN_COMPLETED, CLAIMING));
        transitions.put(RETURN_COMPLETED, Set.of(PENDING_REFUND));
        transitions.put(PENDING_REFUND, Set.of(REFUND_COMPLETED));
        transitions.put(LOST_OR_UNRETURNED, Set.of(CLAIMING));
        transitions.put(CLAIMING, Set.of(CLAIM_COMPLETED));

        // 종료 상태는 전환 불가
        transitions.put(REFUND_COMPLETED, Set.of());
        transitions.put(CLAIM_COMPLETED, Set.of());
        transitions.put(REJECTED, Set.of());
        transitions.put(CANCELLED, Set.of());

        ALLOWED_TRANSITIONS = Collections.unmodifiableMap(transitions);
    }

    ReservationStatus(String description, boolean isReviewable, ReservationStatusSubject statusSubject) {
        this.description = description;
        this.isReviewable = isReviewable;
        this.statusSubject = statusSubject;
    }

    /**
     * 현재 상태에서 전환 가능한 상태 목록 반환
     */
    public Set<ReservationStatus> getAllowedTransitions() {
        return ALLOWED_TRANSITIONS.getOrDefault(this, Set.of());
    }

    /**
     * 특정 상태로 전환 가능한지 확인
     */
    public boolean canTransitionTo(ReservationStatus targetStatus) {
        return getAllowedTransitions().contains(targetStatus);
    }

    /**
     * 취소 가능한 상태인지 확인
     */
    public boolean isCancellable() {
        return this == PENDING_APPROVAL ||
                this == PENDING_PAYMENT ||
                this == PENDING_PICKUP ||
                this == INSPECTING_RENTAL;
    }
}