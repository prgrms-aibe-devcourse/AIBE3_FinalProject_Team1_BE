package com.back.domain.review.review.service;

import com.back.domain.reservation.reservation.entity.Reservation;
import com.back.domain.reservation.reservation.service.ReservationService;
import com.back.domain.review.review.dto.ReviewDto;
import com.back.domain.review.review.dto.ReviewWriteReqBody;
import com.back.domain.review.review.entity.Review;
import com.back.domain.review.review.repository.ReviewRepository;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final ReservationService reservationService;

    @Transactional
    public void writeReview(Long reservationId, ReviewWriteReqBody reqBody, Long authorId) {
        Reservation reservation = reservationService.getById(reservationId);
        // TODO: 예약 상태에 따라 생성 불가 로직 추가 필요
        if (reservation.getReview() != null) {
            throw new ServiceException("400-1","이미 작성된 리뷰가 있습니다.");
        }
        if (reservation.getAuthor() == null || reservation.getAuthor().getId() == null) {
            throw new ServiceException("500-1", "예약 정보가 올바르지 않습니다.");
        }
        if (!reservation.getAuthor().getId().equals(authorId)) {
            throw new ServiceException("403-1", "리뷰를 작성할 권한이 없습니다.");
        }

        Review review = Review.builder()
                .reservation(reservation)
                .comment(reqBody.comment())
                .equipmentScore(reqBody.equipmentScore())
                .kindnessScore(reqBody.kindnessScore())
                .responseTimeScore(reqBody.responseTimeScore())
                .build();

        reviewRepository.save(review);
    }


    public Page<ReviewDto> getPostReviews(Pageable pageable, Long postId){
        Page<Review> page = reviewRepository.findReviewByReservation_Id(pageable, postId);
        List<ReviewDto> content = page.getContent().stream()
                .map(r -> new ReviewDto(
                        r.getId(),
                        r.getEquipmentScore(),
                        r.getKindnessScore(),
                        r.getResponseTimeScore(),
                        r.getComment(),
                        null
                )).toList();
        return new PageImpl<>(content, page.getPageable(), page.getTotalElements());
    }

    public Page<ReviewDto> getMemberReviews(Pageable pageable, Long memberId) {
        Page<Review> page = reviewRepository.findReviewByReservation_Id(pageable, memberId);
        List<ReviewDto> content = page.getContent().stream()
                .map(r -> new ReviewDto(
                        r.getId(),
                        r.getEquipmentScore(),
                        r.getKindnessScore(),
                        r.getResponseTimeScore(),
                        r.getComment(),
                        null
                )).toList();
        return new PageImpl<>(content, page.getPageable(), page.getTotalElements());
    }
}
