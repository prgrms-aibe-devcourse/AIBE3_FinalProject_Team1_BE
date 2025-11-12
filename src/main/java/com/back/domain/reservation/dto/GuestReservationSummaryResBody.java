package com.back.domain.reservation.dto;

import com.back.domain.reservation.common.ReservationDeliveryMethod;
import com.back.domain.reservation.common.ReservationStatus;
import com.back.domain.reservation.entity.Reservation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record GuestReservationSummaryResBody(
        Long reservationId,
        ReservationPostSummaryDto post, // Post 요약 정보
        ReservationStatus status,
        ReservationDeliveryMethod receiveMethod,
        ReservationDeliveryMethod returnMethod,
        String cancelReason,
        String rejectReason,
        LocalDate reservationStartAt,
        LocalDate reservationEndAt,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt,
        List<OptionDto> option, // 선택된 옵션 정보
        int totalAmount // Service에서 계산된 최종 금액
) {
    // 💡 내부 DTO 1: 예약된 게시글 요약 정보
    public record ReservationPostSummaryDto(
            Long postId,
            String title,
            String thumbnailUrl,
            AuthorDto author
    ) {
    }

    public GuestReservationSummaryResBody(
            Reservation reservation,
            ReservationPostSummaryDto postSummary,
            List<OptionDto> optionDtos,
            int calculatedTotalAmount
    ) {
        // 4. 표준 생성자 호출 및 필드 매핑
        this(
                reservation.getId(),
                postSummary, // ⬅️ Service에서 준비된 DTO
                reservation.getStatus(),
                reservation.getReceiveMethod(),
                reservation.getReturnMethod(),
                reservation.getCancelReason(),
                reservation.getRejectReason(),
                reservation.getReservationStartAt(),
                reservation.getReservationEndAt(),
                reservation.getCreatedAt(),
                reservation.getModifiedAt(),
                optionDtos, // ⬅️ Service에서 준비된 DTO 리스트
                calculatedTotalAmount // ⬅️ Service에서 계산된 총액
        );
    }
}