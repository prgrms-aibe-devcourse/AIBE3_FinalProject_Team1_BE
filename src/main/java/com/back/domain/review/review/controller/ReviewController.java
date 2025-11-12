package com.back.domain.review.review.controller;

import com.back.domain.review.review.dto.ReviewDto;
import com.back.domain.review.review.dto.ReviewWriteReqBody;
import com.back.domain.review.review.service.ReviewService;
import com.back.global.security.SecurityUser;
import com.back.standard.util.page.PagePayload;
import com.back.standard.util.page.PageUt;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @PostMapping("/api/v1/reviews/{reservationId}")
    public ResponseEntity<String> write(
            @PathVariable Long reservationId,
            @Valid@RequestBody ReviewWriteReqBody reqBody,
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        reviewService.writeReview(reservationId, reqBody, securityUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body("리뷰가 작성되었습니다.");
    }

    @GetMapping("/api/v1/posts/{postId}/reviews")
    public ResponseEntity<PagePayload<ReviewDto>> getPostReviews(
            @PathVariable Long postId,
            @ParameterObject @PageableDefault(size = 30, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ){
        Page<ReviewDto> pages = reviewService.getPostReviews(pageable, postId);
        return ResponseEntity.ok(PageUt.of(pages));
    }

    @GetMapping("/api/v1/members/{memberId}/reviews")
    public ResponseEntity<PagePayload<ReviewDto>> getMemberReviews(
            @PathVariable Long memberId,
            @ParameterObject @PageableDefault(size = 30, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ){
        Page<ReviewDto> pages = reviewService.getMemberReviews(pageable, memberId);
        return ResponseEntity.ok(PageUt.of(pages));
    }
}
