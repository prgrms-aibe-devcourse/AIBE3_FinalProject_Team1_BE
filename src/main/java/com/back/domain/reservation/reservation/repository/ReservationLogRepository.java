package com.back.domain.reservation.reservation.repository;

import com.back.domain.reservation.reservation.entity.Reservation;
import com.back.domain.reservation.reservation.entity.ReservationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationLogRepository extends JpaRepository<ReservationLog, Long> {
    List<ReservationLog> findByReservation(Reservation reservation);
}
