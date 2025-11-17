package com.back.domain.reservation.dto;

import com.back.domain.reservation.common.ReservationStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateReservationStatusReqBody(
        @NotNull
        ReservationStatus status,
        String cancelReason,
        String rejectReason,
        String claimReason,
        String receiveCarrier,
        String receiveTrackingNumber,
        String returnCarrier,
        String returnTrackingNumber
) {
}
