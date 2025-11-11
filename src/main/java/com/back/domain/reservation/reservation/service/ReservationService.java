package com.back.domain.reservation.reservation.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.reservation.reservation.common.ReservationStatus;
import com.back.domain.reservation.reservation.dto.CreateReservationReqBody;
import com.back.domain.reservation.reservation.dto.GuestReservationSummaryResBody;
import com.back.domain.reservation.reservation.entity.Reservation;
import com.back.domain.reservation.reservation.repository.ReservationRepository;
import com.back.standard.util.page.PagePayload;
import com.back.standard.util.page.PageUt;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReservationService {
    private final ReservationRepository reservationRepository;

    public Reservation create(CreateReservationReqBody reqBody, Member author) {
        // TODO: 게시글 조회
        // Post post = postService.getById(reqBody.postId());

        // 1. 기간 중복 체크
//        validateNoOverlappingReservation(
//                null, // TODO: post.getId()
//                reqBody.reservationStartAt(),
//                reqBody.reservationEndAt()
//        );

        // 2. 같은 게스트의 중복 예약 체크 (게시글 ID 필요)
        // validateNoDuplicateReservation(post.getId(), author.getId());

        Reservation reservation = Reservation.builder()
                .status(ReservationStatus.PENDING_APPROVAL)
                .receiveMethod(reqBody.receiveMethod())
                .receiveCarrier(reqBody.receiveCarrier())
                .receiveTrackingNumber(reqBody.receiveTrackingNumber())
                .receiveAddress1(reqBody.receiveAddress1())
                .receiveAddress2(reqBody.receiveAddress2())
                .returnMethod(reqBody.returnMethod())
                .returnCarrier(reqBody.returnCarrier())
                .returnTrackingNumber(reqBody.returnTrackingNumber())
                .reservationStartAt(reqBody.reservationStartAt())
                .reservationEndAt(reqBody.reservationEndAt())
                .author(author)
                .build();
        return reservationRepository.save(reservation);
    }

    public long count() {
        return reservationRepository.count();
    }

    // 기간 중복 체크
//    private void validateNoOverlappingReservation(
//            Long postId,
//            LocalDateTime startAt,
//            LocalDateTime endAt
//    ) {
//        boolean hasOverlap = reservationRepository.existsOverlappingReservation(
//                postId, startAt, endAt
//        );
//
//        if (hasOverlap) {
//            throw new ServiceException("400-1", "해당 기간에 이미 예약이 있습니다.");
//        }
//    }

    public PagePayload<GuestReservationSummaryResBody> getSentReservations(Member author, Pageable pageable, ReservationStatus status, String keyword) {
        // TODO: post의 제목을 keyword로 검색하도록 수정 필요
        // TODO: QueryDsl로 변경 예정
        Page<Reservation> reservationPage;
        if (status == null) {
            reservationPage = reservationRepository.findByAuthor(author, pageable);
        } else {
            reservationPage = reservationRepository.findByAuthorAndStatus(author, status, pageable);
        }

        Page<GuestReservationSummaryResBody> reservationSummaryDtoPage = reservationPage.map(GuestReservationSummaryResBody::new);

        return PageUt.of(reservationSummaryDtoPage);
    }

//    public PagePayload<ReservationSummaryDto> getReceivedReservations(
//            Post post,
//            Member author,
//            Pageable pageable,
//            ReservationStatus status,
//            String keyword) {
//        // TODO: postId로 게시글 조회 후, 해당 게시글의 호스트와 author 비교 필요
//        Page<Reservation> reservationPage;
//        if (status == null) {
//            reservationPage = reservationRepository.findByPost(post, pageable);
//        } else {
//            reservationPage = reservationRepository.findByPostAndStatus(post, status, pageable);
//        }
//
//        Page<HostReservationSummaryDto> reservationSummaryDtoPage = reservationPage.map(HostReservationSummaryResBody::new);
//
//        return PageUt.of(reservationSummaryDtoPage);
//    }

    public Reservation getById(Long reservationId) {
        return reservationRepository.findById(reservationId).orElseThrow(
                () -> new IllegalArgumentException("해당 예약을 찾을 수 없습니다. id=" + reservationId)
        );
    }
}
