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
    private String claimReason;

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

    // ===== 상태 전환 메서드 =====

    /**
     * 단순 상태 전환 (추가 데이터 없음)
     * - PENDING_PAYMENT (승인)
     * - PENDING_PICKUP (결제 완료)
     * - INSPECTING_RENTAL (대여 검수 시작)
     * - RENTING (대여 시작)
     * - PENDING_RETURN (반납 요청)
     * - RETURN_COMPLETED (반납 완료)
     * - INSPECTING_RETURN (반납 검수)
     * - PENDING_REFUND (환급 예정)
     * - REFUND_COMPLETED (환급 완료)
     * - LOST_OR_UNRETURNED (미반납/분실)
     * - CLAIM_COMPLETED (청구 완료)
     */
    public void changeStatus(ReservationStatus newStatus) {
        validateTransition(newStatus);
        this.status = newStatus;
    }

    /**
     * 거절 (사유 필요)
     */
    public void reject(String reason) {
        validateTransition(ReservationStatus.REJECTED);
        this.status = ReservationStatus.REJECTED;
        this.rejectReason = reason;
    }

    /**
     * 취소 (사유 필요)
     */
    public void cancel(String reason) {
        if (!this.status.isCancellable()) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "현재 상태에서는 취소할 수 없습니다.");
        }
        validateTransition(ReservationStatus.CANCELLED);
        this.status = ReservationStatus.CANCELLED;
        this.cancelReason = reason;
    }

    /**
     * 청구 (사유 필요)
     */
    public void claim(String reason) {
        validateTransition(ReservationStatus.CLAIMING);
        this.status = ReservationStatus.CLAIMING;
        this.claimReason = reason;
    }

    /**
     * 대여 검수 → 반납 대기 (검수 중 문제 발견)
     */
    public void failRentalInspection(String reason) {
        validateTransition(ReservationStatus.PENDING_RETURN);
        this.status = ReservationStatus.PENDING_RETURN;
        this.cancelReason = reason;  // 검수 실패 사유
    }

    /**
     * 배송 시작 (배송 정보 필요)
     */
    public void startShipping(String receiveCarrier, String receiveTrackingNumber) {
        validateTransition(ReservationStatus.SHIPPING);
        validateShippingInfo(receiveCarrier, receiveTrackingNumber);

        this.status = ReservationStatus.SHIPPING;
        this.receiveCarrier = receiveCarrier;
        this.receiveTrackingNumber = receiveTrackingNumber;
    }

    /**
     * 반납 배송 시작 (배송 정보 필요)
     */
    public void startReturning(String returnCarrier, String returnTrackingNumber) {
        validateTransition(ReservationStatus.RETURNING);
        validateShippingInfo(returnCarrier, returnTrackingNumber);

        this.status = ReservationStatus.RETURNING;
        this.returnCarrier = returnCarrier;
        this.returnTrackingNumber = returnTrackingNumber;
    }

    // ===== 유효성 검증 =====

    private void validateTransition(ReservationStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new ServiceException(HttpStatus.BAD_REQUEST,
                    String.format("현재 상태(%s)에서 %s(으)로 전환할 수 없습니다.",
                            this.status.getDescription(),
                            newStatus.getDescription()));
        }
    }

    private void validateShippingInfo(String carrier, String trackingNumber) {
        if (carrier == null || carrier.isBlank()) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "배송사 정보는 필수입니다.");
        }
        if (trackingNumber == null || trackingNumber.isBlank()) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "운송장 번호는 필수입니다.");
        }
    }

    public boolean canTransitionTo(ReservationStatus newStatus) {
        return this.status.canTransitionTo(newStatus);
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