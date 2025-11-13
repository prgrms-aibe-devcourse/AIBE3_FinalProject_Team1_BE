package com.back.domain.reservation.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReservationDeliveryMethod {
    DIRECT("직거래"),
    DELIVERY("택배");

    private final String description;
}
