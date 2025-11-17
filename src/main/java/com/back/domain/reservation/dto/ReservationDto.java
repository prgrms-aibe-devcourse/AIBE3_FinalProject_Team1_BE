package com.back.domain.reservation.dto;

import com.back.domain.reservation.common.ReservationDeliveryMethod;
import com.back.domain.reservation.common.ReservationStatus;
import com.back.domain.reservation.entity.Reservation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ReservationDto(
        Long id,
        Long postId,
        AuthorDto author,
        ReservationStatus status,
        ReservationDeliveryMethod receiveMethod,
        String receiveCarrier,
        String receiveTrackingNumber,
        String receiveAddress1,
        String receiveAddress2,
        ReservationDeliveryMethod returnMethod,
        String returnCarrier,
        String returnTrackingNumber,
        String cancelReason,
        String rejectReason,
        String claimReason,
        LocalDate reservationStartAt,
        LocalDate reservationEndAt,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt,
        List<OptionDto> option,
        List<ReservationLogDto> logs,
        int totalAmount
) {
    public ReservationDto(
            Reservation reservation,
            List<OptionDto> optionDtos,
            List<ReservationLogDto> logDtos,
            int calculatedTotalAmount) {
        this(
                reservation.getId(),
                reservation.getPost().getId(),
                new AuthorDto(reservation.getAuthor()),
                reservation.getStatus(),
                reservation.getReceiveMethod(),
                reservation.getReceiveCarrier(),
                reservation.getReceiveTrackingNumber(),
                reservation.getReceiveAddress1(),
                reservation.getReceiveAddress2(),
                reservation.getReturnMethod(),
                reservation.getReturnCarrier(),
                reservation.getReturnTrackingNumber(),
                reservation.getCancelReason(),
                reservation.getRejectReason(),
                reservation.getClaimReason(),
                reservation.getReservationStartAt(),
                reservation.getReservationEndAt(),
                reservation.getCreatedAt(),
                reservation.getModifiedAt(),
                optionDtos,
                logDtos,
                calculatedTotalAmount
        );
    }
}
