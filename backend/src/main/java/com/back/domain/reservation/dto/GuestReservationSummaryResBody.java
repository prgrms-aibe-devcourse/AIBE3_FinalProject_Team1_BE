package com.back.domain.reservation.dto;

import com.back.domain.reservation.common.ReservationDeliveryMethod;
import com.back.domain.reservation.common.ReservationStatus;
import com.back.domain.reservation.entity.Reservation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record GuestReservationSummaryResBody(
        Long id,
        ReservationPostSummaryDto post, // Post 요약 정보
        ReservationStatus status,
        ReservationDeliveryMethod receiveMethod,
        ReservationDeliveryMethod returnMethod,
        String cancelReason,
        String rejectReason,
        LocalDateTime reservationStartAt,
        LocalDateTime reservationEndAt,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt,
        List<OptionDto> option,
        int totalAmount,
        boolean hasReviewed
) {
    // 예약된 게시글 요약 정보
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
            int calculatedTotalAmount,
            boolean hasReviewed
    ) {
        this(
                reservation.getId(),
                postSummary,
                reservation.getStatus(),
                reservation.getReceiveMethod(),
                reservation.getReturnMethod(),
                reservation.getCancelReason(),
                reservation.getRejectReason(),
                reservation.getReservationStartAt(),
                reservation.getReservationEndAt(),
                reservation.getCreatedAt(),
                reservation.getModifiedAt(),
                optionDtos,
                calculatedTotalAmount,
                hasReviewed
        );
    }
}