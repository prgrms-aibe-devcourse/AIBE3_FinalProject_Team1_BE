package com.back.domain.review.controller;

import com.back.domain.review.dto.ReviewBannedResBody;
import com.back.domain.review.service.ReviewService;
import com.back.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/adm/reviews")
public class ReviewAdmController implements ReviewAdmApi{
    private final ReviewService reviewService;

    @PatchMapping("/{id}/ban")
    public ResponseEntity<RsData<ReviewBannedResBody>> banReview(
            @PathVariable Long id
    ) {
        ReviewBannedResBody reviewBannedResBody = reviewService.banReview(id);
        RsData<ReviewBannedResBody> response = new RsData<>(200, "리뷰가 제재되었습니다.", reviewBannedResBody);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/unban")
    public ResponseEntity<RsData<ReviewBannedResBody>> unbanReview(
            @PathVariable Long id
    ) {
        ReviewBannedResBody reviewBannedResBody = reviewService.unbanReview(id);
        RsData<ReviewBannedResBody> response = new RsData<>(200, "리뷰 제재가 해제되었습니다.", reviewBannedResBody);
        return ResponseEntity.ok(response);
    }
}
