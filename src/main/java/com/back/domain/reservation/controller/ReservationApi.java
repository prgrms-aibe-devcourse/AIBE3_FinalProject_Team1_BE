package com.back.domain.reservation.controller;

import com.back.domain.reservation.common.ReservationStatus;
import com.back.domain.reservation.dto.*;
import com.back.global.rsData.RsData;
import com.back.global.security.SecurityUser;
import com.back.standard.util.page.PagePayload;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Reservation API", description = "예약 관리 API, 인증 필요")
public interface ReservationApi {
    @Operation(
            summary = "예약 생성",
            description = "새로운 예약을 생성합니다. 게스트가 호스트의 게시글에 예약을 신청할 때 사용됩니다."
    )
    ResponseEntity<RsData<ReservationDto>> createReservation(
            @Valid @RequestBody CreateReservationReqBody reqBody,
            @Parameter(hidden = true) @AuthenticationPrincipal SecurityUser securityUser
    );

    @Operation(
            summary = "게스트가 신청한 예약 목록 조회",
            description = "로그인한 사용자가 게스트로서 신청한 예약 목록을 조회합니다. 상태 필터링 및 키워드 검색이 가능합니다."
    )
    ResponseEntity<RsData<PagePayload<GuestReservationSummaryResBody>>> getSentReservations(
            @Parameter(hidden = true) @AuthenticationPrincipal SecurityUser securityUser,
            @Parameter(description = "페이지 정보 (size=5, page=0 기본값)") @PageableDefault(size = 5, page = 0) Pageable pageable,
            @Parameter(description = "예약 상태 필터 (선택)") @RequestParam(required = false) ReservationStatus status,
            @Parameter(description = "검색 키워드 (선택)") @RequestParam(required = false) String keyword
    );

    @Operation(
            summary = "호스트가 받은 예약 목록 조회",
            description = "특정 게시글에 대해 받은 예약 목록을 조회합니다. 해당 게시글의 작성자만 조회 가능합니다."
    )
    ResponseEntity<RsData<PagePayload<HostReservationSummaryResBody>>> getReceivedReservations(
            @Parameter(hidden = true) @AuthenticationPrincipal SecurityUser securityUser,
            @Parameter(description = "게시글 ID", required = true) @PathVariable Long postId,
            @Parameter(description = "페이지 정보 (size=5, page=0 기본값)") @PageableDefault(size = 5, page = 0) Pageable pageable,
            @Parameter(description = "예약 상태 필터 (선택)") @RequestParam(required = false) ReservationStatus status,
            @Parameter(description = "검색 키워드 (선택)") @RequestParam(required = false) String keyword
    );

    @Operation(
            summary = "예약 상세 정보 조회",
            description = "특정 예약의 상세 정보를 조회합니다. 게스트 또는 호스트만 조회 가능합니다."
    )
    ResponseEntity<RsData<ReservationDto>> getReservationDetail(
            @Parameter(description = "예약 ID", required = true) @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal SecurityUser securityUser
    );

    @Operation(
            summary = "예약 상태 변경",
            description = "예약의 상태를 변경합니다. 각 상태 전환은 역할(게스트/호스트)과 현재 상태에 따라 권한이 제한됩니다."
    )
    ResponseEntity<RsData<ReservationDto>> updateReservationStatus(
            @Parameter(description = "예약 ID", required = true) @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal SecurityUser securityUser,
            @Valid @RequestBody UpdateReservationStatusReqBody reqBody
    );

    @Operation(
            summary = "예약 정보 수정",
            description = "예약의 기본 정보(날짜, 배송 방식, 옵션 등)를 수정합니다. 승인 대기 상태에서만 수정 가능하며, 게스트만 수정할 수 있습니다."
    )
    ResponseEntity<RsData<ReservationDto>> updateReservation(
            @Parameter(description = "예약 ID", required = true) @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal SecurityUser securityUser,
            @Valid @RequestBody UpdateReservationReqBody reqBody
    );
}
