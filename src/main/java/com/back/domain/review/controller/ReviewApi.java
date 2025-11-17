package com.back.domain.review.controller;

import com.back.domain.review.dto.ReviewDto;
import com.back.domain.review.dto.ReviewWriteReqBody;
import com.back.global.rsData.RsData;
import com.back.global.security.SecurityUser;
import com.back.standard.util.page.PagePayload;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public interface ReviewApi {
    @Operation(summary = "후기 등록", description = "반납 완료된 건에 대하여 후기를 등록합니다.")
    ResponseEntity<RsData<ReviewDto>> write(
            @PathVariable Long reservationId,
            @Valid @RequestBody ReviewWriteReqBody reqBody,
            @AuthenticationPrincipal SecurityUser securityUser
    );
    @Operation(summary = "게시글 후기 조회", description = "특정 게시글에 작성된 후기를 페이징하여 조회합니다.")
    ResponseEntity<RsData<PagePayload<ReviewDto>>> getPostReviews(
            @PathVariable Long postId,
            @ParameterObject @PageableDefault(size = 30, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    );
    @Operation(summary = "사용자 후기 조회", description = "특정 사용자에 작성된 후기를 페이징하여 조회합니다.")
    ResponseEntity<RsData<PagePayload<ReviewDto>>> getMemberReviews(
            @PathVariable Long memberId,
            @ParameterObject @PageableDefault(size = 30, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    );
}
