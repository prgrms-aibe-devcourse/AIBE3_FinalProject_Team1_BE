package com.back.domain.review.controller;

import com.back.domain.review.dto.ReviewBannedResBody;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Review Admin API", description = "리뷰 관리자 API, 관리자 인증 필요")
public interface ReviewAdmApi {
    @Operation(summary = "리뷰 제재 API", description = "id에 해당하는 리뷰를 제재합니다.")
    ResponseEntity<RsData<ReviewBannedResBody>> banReview(@PathVariable Long id);

    @Operation(summary = "리뷰 제재 해제 API", description = "id에 해당하는 리뷰의 제재를 해제합니다.")
    ResponseEntity<RsData<ReviewBannedResBody>> unbanReview(@PathVariable Long id);
}
