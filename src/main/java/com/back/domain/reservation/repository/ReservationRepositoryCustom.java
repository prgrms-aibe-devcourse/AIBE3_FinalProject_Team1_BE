package com.back.domain.reservation.repository;

import java.time.LocalDate;

public interface ReservationRepositoryCustom {
    boolean existsOverlappingReservation(
            Long postId,
            LocalDate startAt,
            LocalDate endAt,
            Long excludeReservationId
    );
    boolean existsActiveReservation(Long postId, Long authorId);
}
