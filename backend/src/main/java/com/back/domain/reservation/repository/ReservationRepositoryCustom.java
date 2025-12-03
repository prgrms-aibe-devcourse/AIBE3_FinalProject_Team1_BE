package com.back.domain.reservation.repository;

import com.back.domain.member.entity.Member;
import com.back.domain.post.entity.Post;
import com.back.domain.reservation.common.ReservationStatus;
import com.back.domain.reservation.entity.Reservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepositoryCustom {
    boolean existsOverlappingReservation(
            Long postId,
            LocalDateTime startAt,
            LocalDateTime endAt,
            Long excludeReservationId
    );

    boolean existsActiveReservation(Long postId, Long authorId);

    Optional<Reservation> findByIdWithAll(Long id);
    Optional<Reservation> findByIdWithPostAndAuthor(Long id);

    Page<Reservation> findByAuthorWithFetch(Member author,
                                            List<ReservationStatus> status,
                                            String keyword,
                                            Pageable pageable);

    Page<Reservation> findByPostWithFetch(Post post,
                                          ReservationStatus status,
                                          Pageable pageable);
}
