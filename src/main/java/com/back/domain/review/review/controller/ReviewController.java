package com.back.domain.review.review.controller;

import com.back.domain.review.review.dto.ReviewDto;
import com.back.domain.review.review.dto.ReviewWriteReqBody;
import com.back.domain.review.review.service.ReviewService;
import com.back.global.rsData.RsData;
import com.back.standard.util.page.PagePayload;
import com.back.standard.util.page.PageUt;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @PostMapping("/api/v1/reviews/{reservationId}")
    public RsData<Void> write(
            @PathVariable Long reservationId,
            @Valid@RequestBody ReviewWriteReqBody reqBody
    ) {
        // TODO: 예약 상태에 따라 생성 불가 로직 추가 필요
        // TODO: 해당 예약에 이미 등록한 후기가 있으면 중복 생성 불가 로직 추가 필요
        reviewService.writeReview(reservationId, reqBody);
        return RsData.success("리뷰가 작성되었습니다.");
    }

    @GetMapping("/api/v1/posts/{postId}/reviews")
    public RsData<PagePayload<ReviewDto>> getPostReviews(
            @PathVariable Long postId,
            @ParameterObject @PageableDefault(size = 30, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ){
        Page<ReviewDto> pages = reviewService.getPostReviews(pageable, postId);
        return RsData.success("", PageUt.of(pages));
    }

    @GetMapping("/api/v1/members/{memberId}/reviews")
    public RsData<PagePayload<ReviewDto>> getMemberReviews(
            @PathVariable Long memberId,
            @ParameterObject @PageableDefault(size = 30, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ){
        Page<ReviewDto> pages = reviewService.getMemberReviews(pageable, memberId);
        return RsData.success("", PageUt.of(pages));
    }
}
