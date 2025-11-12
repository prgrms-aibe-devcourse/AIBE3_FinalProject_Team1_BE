package com.back.domain.reservation.reservation.dto;

import com.back.domain.reservation.reservation.common.ReservationDeliveryMethod;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record UpdateReservationReqBody(
        @NotNull
        ReservationDeliveryMethod receiveMethod,
        String receiveAddress1,
        String receiveAddress2,

        @NotNull
        ReservationDeliveryMethod returnMethod,

        @Size(max = 5)
        List<Long> optionIds,

        @NotNull
        @Future // 현재 시간 이후일 것
        LocalDate reservationStartAt,
        @NotNull
        @Future
        LocalDate reservationEndAt
) {
}
