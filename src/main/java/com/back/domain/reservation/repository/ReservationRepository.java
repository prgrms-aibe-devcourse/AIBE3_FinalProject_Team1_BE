package com.back.domain.reservation.repository;

import com.back.domain.member.member.entity.Member;
import com.back.domain.post.post.entity.Post;
import com.back.domain.reservation.common.ReservationStatus;
import com.back.domain.reservation.entity.Reservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    Page<Reservation> findByAuthor(Member author, Pageable pageable);
    Page<Reservation> findByAuthorAndStatus(Member author, ReservationStatus status, Pageable pageable);

    Page<Reservation> findByPost(Post post, Pageable pageable);
    Page<Reservation> findByPostAndStatus(Post post, ReservationStatus status, Pageable pageable);

    @Query("""
    SELECT r FROM Reservation r
    LEFT JOIN FETCH r.reservationOptions ro
    LEFT JOIN FETCH ro.postOption
    WHERE r.id = :id
    """)
    Optional<Reservation> findByIdWithOptions(@Param("id") Long id);
}
