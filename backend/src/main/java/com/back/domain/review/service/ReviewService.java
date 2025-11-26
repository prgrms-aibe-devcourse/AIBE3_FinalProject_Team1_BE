package com.back.domain.review.service;

import com.back.domain.reservation.entity.Reservation;
import com.back.domain.reservation.repository.ReservationRepository;
import com.back.domain.review.dto.ReviewBannedResBody;
import com.back.domain.review.dto.ReviewDto;
import com.back.domain.review.dto.ReviewWriteReqBody;
import com.back.domain.review.entity.Review;
import com.back.domain.review.repository.ReviewQueryRepository;
import com.back.domain.review.repository.ReviewRepository;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final ReviewQueryRepository reviewQueryRepository;
    private final ReservationRepository reservationRepository;

    @Transactional
    public Review writeReview(Long reservationId, ReviewWriteReqBody reqBody, Long authorId) {
        Reservation reservation = reservationRepository.findById(reservationId).orElseThrow(
                () -> new ServiceException(HttpStatus.NOT_FOUND, "예약 정보를 찾을 수 없습니다.")
        );
        if (!reservation.getAuthor().getId().equals(authorId)) {
            throw new ServiceException(HttpStatus.FORBIDDEN, "리뷰를 작성할 권한이 없습니다.");
        }
        boolean exists = reviewRepository.existsByReservationId(reservationId);
        if (exists) {
            throw new ServiceException(HttpStatus.CONFLICT, "이미 작성된 리뷰가 있습니다.");
        }
        if (!reservation.getStatus().isReviewable()) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "리뷰를 작성할 수 없는 예약 상태입니다.");
        }

        return reviewRepository.save(Review.create(reservation, reqBody));
    }


    public Page<ReviewDto> getPostReviews(Pageable pageable, Long postId){
        return reviewQueryRepository.getPostReceivedReviews(pageable, postId);
    }

    public Page<ReviewDto> getMemberReviews(Pageable pageable, Long memberId) {
        return reviewQueryRepository.getMemberReceivedReviews(pageable, memberId);
    }

    @Transactional
    public ReviewBannedResBody banReview(Long id) {
        Review review = reviewRepository.findById(id).orElseThrow(
                () -> new ServiceException(HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다.")
        );
        if (review.isBanned()) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "이미 제재된 리뷰입니다.");
        }
        review.ban();
        return ReviewBannedResBody.of(review);
    }

    @Transactional
    public ReviewBannedResBody unbanReview(Long id) {
        Review review = reviewRepository.findById(id).orElseThrow(
                () -> new ServiceException(HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다.")
        );
        if (!review.isBanned()) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "제재되지 않은 리뷰입니다.");
        }
        review.unban();
        return ReviewBannedResBody.of(review);
    }
}
