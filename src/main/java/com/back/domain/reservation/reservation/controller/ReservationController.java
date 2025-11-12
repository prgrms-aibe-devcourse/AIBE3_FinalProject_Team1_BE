package com.back.domain.reservation.reservation.controller;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.service.MemberService;
import com.back.domain.reservation.reservation.common.ReservationStatus;
import com.back.domain.reservation.reservation.dto.*;
import com.back.domain.reservation.reservation.service.ReservationService;
import com.back.global.security.SecurityUser;
import com.back.standard.util.page.PagePayload;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
public class ReservationController {
    private final ReservationService reservationService;
    private final MemberService memberService;

    @Transactional
    @PostMapping
    public ResponseEntity<String> createReservation(
            @Valid @RequestBody CreateReservationReqBody ReqBody,
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        Member author = memberService.getById(securityUser.getId());

        Long reservationId = reservationService.create(ReqBody, author);

        return ResponseEntity.status(HttpStatus.CREATED).body("%d번 예약이 생성되었습니다".formatted(reservationId));
    }

    @Transactional(readOnly = true)
    @GetMapping("/sent")
    public ResponseEntity<PagePayload<GuestReservationSummaryResBody>> getSentReservations(
            @AuthenticationPrincipal SecurityUser securityUser,
            @PageableDefault(size = 5, page = 0)Pageable pageable,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) String keyword
            ) {
        Member author = memberService.getById(securityUser.getId());

        PagePayload<GuestReservationSummaryResBody> reservations = reservationService.getSentReservations(author, pageable, status, keyword);

        return ResponseEntity.ok(reservations);
    }

    @Transactional(readOnly = true)
    @GetMapping("/received/{postId}")
    public ResponseEntity<PagePayload<HostReservationSummaryResBody>> getReceivedReservations(
            @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable Long postId,
            @PageableDefault(size = 5, page = 0)Pageable pageable,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) String keyword
    ) {
        Member member = memberService.getById(securityUser.getId());
        PagePayload<HostReservationSummaryResBody> reservations = reservationService.getReceivedReservations(postId, member, pageable, status, keyword);

        return ResponseEntity.ok(reservations);
    }

    @Transactional(readOnly = true)
    @GetMapping("/{reservationId}")
    public ResponseEntity<ReservationDto> getReservationDetail(
            @PathVariable Long reservationId,
            @AuthenticationPrincipal SecurityUser securityUser
            ) {
        ReservationDto reservationDto = reservationService.getReservationDtoById(reservationId, securityUser.getId());
        return ResponseEntity.ok(reservationDto);
    }

    @Transactional
    @PatchMapping("/{reservationId}/status")
    public ResponseEntity<String> updateReservationStatus(
            @PathVariable Long reservationId,
            @AuthenticationPrincipal SecurityUser securityUser,
            @Valid @RequestBody UpdateReservationStatusReqBody reqBody ) {
        reservationService.updateReservationStatus(reservationId, securityUser.getId(), reqBody);
        return ResponseEntity.ok("%d번 예약 상태가 업데이트 되었습니다.".formatted(reservationId));
    }

    @Transactional
    @PutMapping("/{reservationId}")
    public ResponseEntity<String> updateReservation(
            @PathVariable Long reservationId,
            @AuthenticationPrincipal SecurityUser securityUser,
            @Valid @RequestBody UpdateReservationReqBody reqBody ) {
        reservationService.updateReservation(reservationId, securityUser.getId(), reqBody);
        return ResponseEntity.ok("%d번 예약이 수정되었습니다.".formatted(reservationId));
    }
}
