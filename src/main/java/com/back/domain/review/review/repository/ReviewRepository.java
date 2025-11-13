package com.back.domain.review.review.repository;

import com.back.domain.review.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    // TODO: Reservation에 연결된 post로 리뷰 찾기 + post의 author로 리뷰 찾기
    Page<Review> findReviewByReservation_Id(Pageable pageable, Long postId);

    boolean existsByReservationId(Long reservationId);
}
