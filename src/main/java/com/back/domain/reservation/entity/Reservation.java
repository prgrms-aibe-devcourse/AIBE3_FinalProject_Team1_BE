package com.back.domain.reservation.entity;


import com.back.domain.member.entity.Member;
import com.back.domain.post.entity.Post;
import com.back.domain.post.entity.PostOption;
import com.back.domain.reservation.common.ReservationDeliveryMethod;
import com.back.domain.reservation.common.ReservationStatus;
import com.back.global.exception.ServiceException;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Reservation extends BaseEntity {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @Enumerated(EnumType.STRING)
    private ReservationDeliveryMethod receiveMethod;
    private String receiveCarrier;
    private String receiveTrackingNumber;
    private String receiveAddress1;
    private String receiveAddress2;

    @Enumerated(EnumType.STRING)
    private ReservationDeliveryMethod returnMethod;
    private String returnCarrier;
    private String returnTrackingNumber;

    private String cancelReason;
    private String rejectReason;

    private LocalDate reservationStartAt;
    private LocalDate reservationEndAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private Member author;

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReservationOption> reservationOptions = new ArrayList<>();

    public void addAllOptions(List<ReservationOption> reservationOptions) {
        this.reservationOptions.addAll(reservationOptions);
    }

    public Reservation(
            ReservationStatus status,
            ReservationDeliveryMethod receiveMethod,
            String receiveAddress1,
            String receiveAddress2,
            ReservationDeliveryMethod returnMethod,
            LocalDate reservationStartAt,
            LocalDate reservationEndAt,
            Member author,
            Post post
    ) {
        this.status = status;
        this.receiveMethod = receiveMethod;
        this.receiveAddress1 = receiveAddress1;
        this.receiveAddress2 = receiveAddress2;
        this.returnMethod = returnMethod;
        this.reservationStartAt = reservationStartAt;
        this.reservationEndAt = reservationEndAt;
        this.author = author;
        this.post = post;
    }

//    public static Reservation createPendingReservation(
//            ReservationDeliveryMethod receiveMethod,
//            String receiveAddress1,
//            String receiveAddress2,
//            ReservationDeliveryMethod returnMethod,
//            LocalDate reservationStartAt,
//            LocalDate reservationEndAt,
//            Member author,
//            Post post
//    ) {
//        return new Reservation(
//                ReservationStatus.PENDING_APPROVAL,
//                receiveMethod,
//                receiveAddress1,
//                receiveAddress2,
//                returnMethod,
//                reservationStartAt,
//                reservationEndAt,
//                author,
//                post
//        );
//    }

    // ===== 상태 전환 메서드 =====

    // 승인 대기 -> 결제 대기 (호스트 승인)
    public void approve() {
        validateTransition(ReservationStatus.PENDING_PAYMENT);
        this.status = ReservationStatus.PENDING_PAYMENT;
    }

    // 승인 대기 -> 승인 거절
    public void reject(String reason) {
        validateTransition(ReservationStatus.REJECTED);
        this.status = ReservationStatus.REJECTED;
        this.rejectReason = reason;
    }

    // 여러 단계에서 -> 예약 취소
    public void cancel(String reason) {
        if (!canCancel()) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "현재 상태에서는 취소할 수 없습니다.");
        }
        this.status = ReservationStatus.CANCELLED;
        this.cancelReason = reason;
    }

    // 결제 대기 -> 수령 대기 (결제 완료)
    public void completePayment() {
        validateTransition(ReservationStatus.PENDING_PICKUP);
        this.status = ReservationStatus.PENDING_PICKUP;
    }

    // 수령 대기 -> 배송 중 (택배) - 배송 정보 입력
    public void startShipping(String receiveCarrier, String receiveTrackingNumber) {
        validateTransition(ReservationStatus.SHIPPING);

        if (receiveCarrier == null || receiveCarrier.isBlank()) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "배송사 정보는 필수입니다.");
        }
        if (receiveTrackingNumber == null || receiveTrackingNumber.isBlank()) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "운송장 번호는 필수입니다.");
        }

        this.status = ReservationStatus.SHIPPING;
        this.receiveCarrier = receiveCarrier;
        this.receiveTrackingNumber = receiveTrackingNumber;
    }

    // 수령 대기 -> 대여 검수 (직거래) or 배송 중 -> 대여 검수 (배송 완료)
    public void startRentalInspection() {
        validateTransition(ReservationStatus.INSPECTING_RENTAL);
        this.status = ReservationStatus.INSPECTING_RENTAL;
    }

    // 대여 검수 -> 대여 중 (검수 정상)
    public void startRenting() {
        validateTransition(ReservationStatus.RENTING);
        this.status = ReservationStatus.RENTING;
    }

    // 대여 중 -> 반납 대기 (반납 요청)
    public void requestReturn() {
        validateTransition(ReservationStatus.PENDING_RETURN);
        this.status = ReservationStatus.PENDING_RETURN;
    }

    // 대여 중 -> 미반납/분실
    public void markAsLost() {
        validateTransition(ReservationStatus.LOST_OR_UNRETURNED);
        this.status = ReservationStatus.LOST_OR_UNRETURNED;
    }

    // 반납 대기 -> 반납 중 (택배) - 반납 배송 정보 입력
    public void startReturning(String returnCarrier, String returnTrackingNumber) {
        validateTransition(ReservationStatus.RETURNING);

        if (returnCarrier == null || returnCarrier.isBlank()) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "배송사 정보는 필수입니다.");
        }
        if (returnTrackingNumber == null || returnTrackingNumber.isBlank()) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "운송장 번호는 필수입니다.");
        }

        this.status = ReservationStatus.RETURNING;
        this.returnCarrier = returnCarrier;
        this.returnTrackingNumber = returnTrackingNumber;
    }

    // 반납 대기 -> 반납 완료 (직거래) or 반납 중 -> 반납 완료 (배송 완료)
    public void completeReturn() {
        validateTransition(ReservationStatus.RETURN_COMPLETED);
        this.status = ReservationStatus.RETURN_COMPLETED;
    }

    // 반납 완료 -> 반납 검수
    public void startReturnInspection() {
        validateTransition(ReservationStatus.INSPECTING_RETURN);
        this.status = ReservationStatus.INSPECTING_RETURN;
    }

    // 반납 검수 -> 환급 예정 (검수 정상)
    public void scheduleRefund() {
        validateTransition(ReservationStatus.PENDING_REFUND);
        this.status = ReservationStatus.PENDING_REFUND;
    }

    // 환급 예정 -> 환급 완료
    public void completeRefund() {
        validateTransition(ReservationStatus.REFUND_COMPLETED);
        this.status = ReservationStatus.REFUND_COMPLETED;
    }

    // 미반납/분실 or 반납 검수 -> 청구 진행
    public void startClaim() {
        validateTransition(ReservationStatus.CLAIMING);
        this.status = ReservationStatus.CLAIMING;
    }

    // 청구 진행 -> 청구 완료
    public void completeClaim() {
        validateTransition(ReservationStatus.CLAIM_COMPLETED);
        this.status = ReservationStatus.CLAIM_COMPLETED;
    }

    // ===== 유효성 검증 =====

    private void validateTransition(ReservationStatus newStatus) {
        Set<ReservationStatus> allowedTransitions = getAllowedTransitions();

        if (!allowedTransitions.contains(newStatus)) {
            throw new ServiceException(HttpStatus.BAD_REQUEST,
                    String.format("현재 상태(%s)에서 %s(으)로 전환할 수 없습니다.",
                            this.status.getDescription(),
                            newStatus.getDescription()));
        }
    }

    private Set<ReservationStatus> getAllowedTransitions() {
        return switch (this.status) {
            case PENDING_APPROVAL -> Set.of(
                    ReservationStatus.PENDING_PAYMENT,  // 승인
                    ReservationStatus.REJECTED,          // 거절
                    ReservationStatus.CANCELLED          // 취소
            );

            case PENDING_PAYMENT -> Set.of(
                    ReservationStatus.PENDING_PICKUP,    // 결제 완료
                    ReservationStatus.CANCELLED          // 취소
            );

            case PENDING_PICKUP -> Set.of(
                    ReservationStatus.SHIPPING,              // 택배 배송
                    ReservationStatus.INSPECTING_RENTAL,     // 직거래 검수
                    ReservationStatus.CANCELLED              // 취소
            );

            case SHIPPING -> Set.of(
                    ReservationStatus.INSPECTING_RENTAL  // 배송 완료 후 검수
            );

            case INSPECTING_RENTAL -> Set.of(
                    ReservationStatus.RENTING,           // 검수 정상
                    ReservationStatus.CANCELLED          // 검수 취소
            );

            case RENTING -> Set.of(
                    ReservationStatus.PENDING_RETURN,        // 반납 요청
                    ReservationStatus.LOST_OR_UNRETURNED     // 미반납/분실
            );

            case PENDING_RETURN -> Set.of(
                    ReservationStatus.RETURNING,         // 택배 반납
                    ReservationStatus.RETURN_COMPLETED   // 직거래 반납 완료
            );

            case RETURNING -> Set.of(
                    ReservationStatus.RETURN_COMPLETED   // 반납 완료
            );

            case RETURN_COMPLETED -> Set.of(
                    ReservationStatus.INSPECTING_RETURN  // 반납 검수
            );

            case INSPECTING_RETURN -> Set.of(
                    ReservationStatus.PENDING_REFUND    // 검수 정상 -> 환급
                    // ReservationStatus.CLAIMING           // 손상/분실 -> 청구 (로직 설계에 있지 않아 일단 비활성화)
            );

            case PENDING_REFUND -> Set.of(
                    ReservationStatus.REFUND_COMPLETED   // 환급 완료
            );

            case LOST_OR_UNRETURNED -> Set.of(
                    ReservationStatus.CLAIMING           // 청구 진행
            );

            case CLAIMING -> Set.of(
                    ReservationStatus.CLAIM_COMPLETED    // 청구 완료
            );

            // 종료 상태
            case REJECTED, CANCELLED, REFUND_COMPLETED, CLAIM_COMPLETED -> Set.of();
        };
    }

    private boolean canCancel() {
        // 취소 가능한 상태 (흐름도 기준)
        return this.status == ReservationStatus.PENDING_APPROVAL ||
                this.status == ReservationStatus.PENDING_PAYMENT ||
                this.status == ReservationStatus.PENDING_PICKUP ||
                this.status == ReservationStatus.INSPECTING_RENTAL;
    }

    public boolean canTransitionTo(ReservationStatus newStatus) {
        return getAllowedTransitions().contains(newStatus);
    }

    public boolean isModifiable() {
        return this.status == ReservationStatus.PENDING_APPROVAL;
    }

    public void updateDetails(
            ReservationDeliveryMethod receiveMethod,
            String receiveAddress1,
            String receiveAddress2,
            ReservationDeliveryMethod returnMethod,
            LocalDate reservationStartAt,
            LocalDate reservationEndAt,
            List<PostOption> selectedOptions) {
        this.receiveMethod = receiveMethod;
        this.receiveAddress1 = receiveAddress1;
        this.receiveAddress2 = receiveAddress2;
        this.returnMethod = returnMethod;
        this.reservationStartAt = reservationStartAt;
        this.reservationEndAt = reservationEndAt;
        this.reservationOptions.clear();
        selectedOptions.forEach(option ->
                this.reservationOptions.add(new ReservationOption(this, option))
        );
    }
}