package com.back.domain.reservation.controller;

import com.back.domain.member.entity.Member;
import com.back.domain.member.service.MemberService;
import com.back.domain.reservation.common.ReservationStatus;
import com.back.domain.reservation.dto.*;
import com.back.domain.reservation.service.ReservationService;
import com.back.global.rsData.RsData;
import com.back.global.security.SecurityUser;
import com.back.standard.util.page.PagePayload;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
public class ReservationController implements ReservationApi {
    private final ReservationService reservationService;
    private final MemberService memberService;

    @PostMapping
    public ResponseEntity<RsData<ReservationDto>> createReservation(
            @Valid @RequestBody CreateReservationReqBody ReqBody,
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        Member author = memberService.getById(securityUser.getId());

        ReservationDto reservationDto = reservationService.create(ReqBody, author);

        return ResponseEntity.status(HttpStatus.CREATED).body(new RsData<>(HttpStatus.CREATED, "%d번 예약이 생성되었습니다".formatted(reservationDto.id()), reservationDto));
    }

    @GetMapping("/sent")
    public ResponseEntity<RsData<PagePayload<GuestReservationSummaryResBody>>> getSentReservations(
            @AuthenticationPrincipal SecurityUser securityUser,
            @PageableDefault(size = 5, page = 0)Pageable pageable,
            @RequestParam(required = false) List<ReservationStatus> status,
            @RequestParam(required = false) String keyword
            ) {
        log.info("keyword = {}", keyword);
        log.info("status = {}", status);
        Member author = memberService.getById(securityUser.getId());

        PagePayload<GuestReservationSummaryResBody> reservations = reservationService.getSentReservations(author, pageable, status, keyword);

        return ResponseEntity.ok(new RsData<>(HttpStatus.OK, "%d번 게스트가 등록한 예약 목록입니다.".formatted(author.getId()), reservations));
    }

    @GetMapping("/received/{postId}")
    public ResponseEntity<RsData<PagePayload<HostReservationSummaryResBody>>> getReceivedReservations(
            @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable Long postId,
            @PageableDefault(size = 5, page = 0)Pageable pageable,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) String keyword
    ) {

        Member member = memberService.getById(securityUser.getId());
        PagePayload<HostReservationSummaryResBody> reservations = reservationService.getReceivedReservations(postId, member, pageable, status, keyword);

        return ResponseEntity.ok(new RsData<>(HttpStatus.OK, "%d번 게시글에 대한 예약 목록입니다.".formatted(postId), reservations));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RsData<ReservationDto>> getReservationDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal SecurityUser securityUser
            ) {
        ReservationDto reservationDto = reservationService.getReservationDtoById(id, securityUser.getId());
        return ResponseEntity.ok(new RsData<>(HttpStatus.OK, "%d번 예약 상세 정보입니다.".formatted(id), reservationDto));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<RsData<ReservationDto>> updateReservationStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal SecurityUser securityUser,
            @Valid @RequestBody UpdateReservationStatusReqBody reqBody ) {
        ReservationDto reservationDto = reservationService.updateReservationStatus(id, securityUser.getId(), reqBody);
        return ResponseEntity.ok(new RsData<>(HttpStatus.OK, "%d번 예약 상태가 업데이트 되었습니다.".formatted(id), reservationDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RsData<ReservationDto>> updateReservation(
            @PathVariable Long id,
            @AuthenticationPrincipal SecurityUser securityUser,
            @Valid @RequestBody UpdateReservationReqBody reqBody ) {
        ReservationDto reservationDto = reservationService.updateReservation(id, securityUser.getId(), reqBody);
        return ResponseEntity.ok(new RsData<>(HttpStatus.OK, "%d번 예약이 수정되었습니다.".formatted(id), reservationDto));
    }

    @GetMapping("/sent/status")
    public ResponseEntity<RsData<ReservationStatusResBody>> getSentReservationsStatus(
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        Member author = memberService.getById(securityUser.getId());

        ReservationStatusResBody statusCount = reservationService.getSentReservationsStatusCount(author);

        return ResponseEntity.ok(new RsData<>(HttpStatus.OK, "%d번 게스트의 예약 상태별 개수입니다.".formatted(securityUser.getId()), statusCount));
    }
}
