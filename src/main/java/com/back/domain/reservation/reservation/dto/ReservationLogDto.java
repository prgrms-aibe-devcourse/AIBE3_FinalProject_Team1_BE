package com.back.domain.reservation.reservation.dto;

import com.back.domain.reservation.reservation.common.ReservationStatus;
import com.back.domain.reservation.reservation.entity.ReservationLog;

import java.time.LocalDateTime;

public record ReservationLogDto(
        Long id,
        ReservationStatus status,
        LocalDateTime createdAt
) {
    public ReservationLogDto(ReservationLog log) {
        this(
                log.getId(),
                log.getStatus(),
                log.getCreatedAt()
        );
    }
}
