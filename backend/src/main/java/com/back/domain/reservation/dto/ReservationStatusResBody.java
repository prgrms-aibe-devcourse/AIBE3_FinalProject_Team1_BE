package com.back.domain.reservation.dto;

import com.back.domain.reservation.common.ReservationStatus;

import java.util.HashMap;
import java.util.Map;

public record ReservationStatusResBody(
        Map<ReservationStatus, Integer> statusCounts,
        Integer totalCount
) {
    public ReservationStatusResBody {
        if (statusCounts == null) {
            statusCounts = new HashMap<>();
        }
        if (totalCount == null) {
            totalCount = 0;
        }
    }

    public Integer getCount(ReservationStatus status) {
        return statusCounts.getOrDefault(status, 0);
    }

    public boolean hasReservations() {
        return totalCount > 0;
    }
}
