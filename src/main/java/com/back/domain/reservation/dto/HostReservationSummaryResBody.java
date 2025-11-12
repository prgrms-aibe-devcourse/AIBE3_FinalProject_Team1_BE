package com.back.domain.reservation.dto;

import com.back.domain.reservation.common.ReservationDeliveryMethod;
import com.back.domain.reservation.common.ReservationStatus;
import com.back.domain.reservation.entity.Reservation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record HostReservationSummaryResBody(
        Long reservationId,
        Long postId,
        ReservationStatus status,
        ReservationDeliveryMethod receiveMethod,
        ReservationDeliveryMethod returnMethod,
        String cancelReason,
        String rejectReason,
        LocalDate reservationStartAt,
        LocalDate reservationEndAt,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt,
        List<OptionDto> option,
        int totalAmount
) {
    public HostReservationSummaryResBody(Reservation reservation, List<OptionDto> optionDtos, int calculatedTotalAmount) {
        this(
                reservation.getId(),
                reservation.getPost().getId(),
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
                calculatedTotalAmount
        );
    }
}
