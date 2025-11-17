package com.back.domain.reservation.dto;

import com.back.domain.reservation.common.ReservationDeliveryMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record CreateReservationReqBody(
        @NotNull
        ReservationDeliveryMethod receiveMethod,
        String receiveAddress1,
        String receiveAddress2,
        @NotNull
        ReservationDeliveryMethod returnMethod,
        @NotNull
        LocalDate reservationStartAt,
        @NotNull
        LocalDate reservationEndAt,
        @NotNull
        Long postId,
        @Size(max = 5)
        List<Long> optionIds
) {
}
