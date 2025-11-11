package com.back.domain.review.review.service;

import com.back.domain.reservation.reservation.entity.Reservation;
import com.back.domain.reservation.reservation.repository.ReservationRepository;
import com.back.domain.review.review.dto.ReviewDto;
import com.back.domain.review.review.dto.ReviewWriteReqBody;
import com.back.domain.review.review.entity.Review;
import com.back.domain.review.review.repository.ReviewRepository;
import com.back.global.exception.ServiceException;
import com.back.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final ReservationRepository reservationRepository;


    public RsData<Void> writeReview(Long reservationId, ReviewWriteReqBody reqBody) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ServiceException("400-1", "존재하지 않는 예약입니다."));

        Review review = Review.builder()
                .reservation(reservation)  // reservation 설정 추가
                .comment(reqBody.comment())
                .equipmentScore(reqBody.equipmentScore())
                .kindnessScore(reqBody.kindnessScore())
                .responseTimeScore(reqBody.responseTimeScore())
                .build();

        reviewRepository.save(review);
        return RsData.success("리뷰가 작성되었습니다.");
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
