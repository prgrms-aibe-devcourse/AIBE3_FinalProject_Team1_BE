package com.back.domain.reservation.service;


import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import com.back.domain.post.common.ReceiveMethod;
import com.back.domain.post.common.ReturnMethod;
import com.back.domain.post.entity.Post;
import com.back.domain.post.entity.PostOption;
import com.back.domain.post.service.PostService;
import com.back.domain.reservation.common.ReservationDeliveryMethod;
import com.back.domain.reservation.common.ReservationStatus;
import com.back.domain.reservation.dto.*;
import com.back.domain.reservation.entity.Reservation;
import com.back.domain.reservation.entity.ReservationLog;
import com.back.domain.reservation.entity.ReservationOption;
import com.back.domain.reservation.repository.ReservationLogRepository;
import com.back.domain.reservation.repository.ReservationQueryRepository;
import com.back.domain.reservation.repository.ReservationRepository;
import com.back.domain.review.repository.ReviewQueryRepository;
import com.back.global.exception.ServiceException;
import com.back.standard.util.page.PagePayload;
import com.back.standard.util.page.PageUt;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final ReservationQueryRepository reservationQueryRepository;
    private final ReservationLogRepository reservationLogRepository;
    private final ReviewQueryRepository reviewQueryRepository;
    private final MemberRepository memberRepository;
    private final PostService postService;

    public ReservationDto create(CreateReservationReqBody reqBody, Member author) {
        Post post = postService.getById(reqBody.postId());

        // 기간 중복 체크
        validateNoOverlappingReservation(
                post.getId(),
                reqBody.reservationStartAt(),
                reqBody.reservationEndAt(),
                null
        );

        // 자신의 게시글에 대한 예약 금지
        if (post.getAuthor().getId().equals(author.getId())) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "자신의 게시글에 대한 예약은 불가능합니다.");
        }

        // 전달 방식 유효성 체크
        validateDeliveryMethods(post, reqBody.receiveMethod(), reqBody.returnMethod());

        // 배송 정보 유효성 체크
        validateDeliveryInfo(reqBody.receiveMethod(), reqBody.receiveAddress1(), reqBody.receiveAddress2());

        // 선택된 PostOption 엔티티 조회 및 유효성 검증
        List<PostOption> selectedOptions = getOptionsByIds(post.getId(), reqBody.optionIds());

        // Reservation 엔티티 빌드
        Reservation reservation = new Reservation(
                ReservationStatus.PENDING_APPROVAL,
                reqBody.receiveMethod(),
                reqBody.receiveAddress1(),
                reqBody.receiveAddress2(),
                reqBody.returnMethod(),
                reqBody.reservationStartAt(),
                reqBody.reservationEndAt(),
                author,
                post
        );

        // reservationOption 리스트 생성 및 설정
        if (!selectedOptions.isEmpty()) {
            List<ReservationOption> reservationOptions = selectedOptions.stream()
                    .map(postOption -> new ReservationOption(reservation, postOption))
                    .toList();

            // Reservation의 리스트 필드에 추가 (addAllOptions 사용)
            reservation.addAllOptions(reservationOptions);
        }

        Reservation r = reservationRepository.save(reservation);
        return convertToReservationDto(r);
    }

    public long count() {
        return reservationRepository.count();
    }

    // 기간 중복 체크
    private void validateNoOverlappingReservation(Long postId, LocalDateTime start, LocalDateTime end, Long excludeId) {
        boolean hasOverlap = reservationQueryRepository.existsOverlappingReservation(
                postId, start, end, excludeId
        );

        if (hasOverlap) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "해당 기간에 이미 예약이 있습니다.");
        }
    }

    private void validateDeliveryMethods(Post post, ReservationDeliveryMethod receiveMethod, ReservationDeliveryMethod returnMethod) {
        // 수령 방식 (Receive Method) 검증
        if (!isReceiveMethodAllowed(post.getReceiveMethod(), receiveMethod)) {
            throw new ServiceException(
                    HttpStatus.BAD_REQUEST,
                    "게시글에서 허용하는 수령 방식(%s)이 아닙니다.".formatted(post.getReceiveMethod().getDescription())
            );
        }

        // 반납 방식 (Return Method) 검증
        if (!isReturnMethodAllowed(post.getReturnMethod(), returnMethod)) {
            throw new ServiceException(
                    HttpStatus.BAD_REQUEST,
                    "게시글에서 허용하는 반납 방식(%s)이 아닙니다.".formatted(post.getReturnMethod().getDescription())
            );
        }
    }

    private boolean isReceiveMethodAllowed(ReceiveMethod postMethod, ReservationDeliveryMethod reqMethod) {
        // ANY인 경우, 모든 방식 허용
        if (postMethod == ReceiveMethod.ANY) {
            return true;
        }

        // Enum 이름(문자열)을 비교하여 동일한지 확인
        return postMethod.name().equals(reqMethod.name());
    }

    private boolean isReturnMethodAllowed(ReturnMethod postMethod, ReservationDeliveryMethod reqMethod) {
        // ANY인 경우, 모든 방식 허용
        if (postMethod == ReturnMethod.ANY) {
            return true;
        }

        // Enum 이름(문자열)을 비교하여 동일한지 확인
        return postMethod.name().equals(reqMethod.name());
    }

    private List<PostOption> getOptionsByIds(Long postId, List<Long> optionIds) {
        if (optionIds == null || optionIds.isEmpty()) {
            return List.of(); // 옵션이 없으면 빈 리스트 반환
        }

        // 옵션 엔티티들을 조회 (PostService에 위임)
        List<PostOption> options = postService.getAllOptionsById(optionIds);

        // 유효성 검증: 개수 일치 확인
        if (options.size() != optionIds.size()) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "선택된 옵션 중 유효하지 않은 옵션이 포함되어 있습니다."); // 400-3, 400-4와 충돌되지 않도록 코드 변경
        }

        // 해당 게시글의 옵션인지 검증
        boolean allBelongToPost = options.stream()
                .allMatch(option -> option.getPost().getId().equals(postId));

        if (!allBelongToPost) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "선택된 옵션은 해당 게시글의 옵션이 아닙니다."); // 400-3, 400-4와 충돌되지 않도록 코드 변경
        }

        return options;
    }

    public PagePayload<GuestReservationSummaryResBody> getSentReservations(
            Member author,
            Pageable pageable,
            ReservationStatus status,
            String keyword) {

        Page<Reservation> reservationPage = reservationQueryRepository.findByAuthorWithFetch(author, status, keyword, pageable);

        // 1. 페이지에 포함된 모든 예약 ID를 추출 (단 1회 실행)
        List<Long> reservationIds = reservationPage.getContent().stream()
                .map(Reservation::getId)
                .toList();

        // 2. 추출된 ID 목록을 사용하여 리뷰가 작성된 ID Set을 DB에서 조회 (단 1회 실행)
        Set<Long> reviewedReservationIds = reviewQueryRepository.findReviewedReservationIds(reservationIds, author.getId());

        // 이제 Lazy Loading 없이 바로 접근 가능
        Page<GuestReservationSummaryResBody> reservationSummaryDtoPage = reservationPage.map(reservation -> {
            Post post = reservation.getPost();
            List<ReservationOption> options = reservation.getReservationOptions();

            int totalAmount = calculateTotalAmount(reservation, post, options);
            GuestReservationSummaryResBody.ReservationPostSummaryDto postSummary = createPostSummaryDto(post);

            List<OptionDto> optionDtos = options.stream()
                    .map(ro -> new OptionDto(
                            ro.getPostOption().getId(),
                            ro.getPostOption().getName()
                    ))
                    .toList();

            // 3. Set에 해당 예약 ID가 포함되어 있는지 확인하여 hasReview 설정
            boolean hasReview = reviewedReservationIds.contains(reservation.getId());

            return new GuestReservationSummaryResBody(
                    reservation,
                    postSummary,
                    optionDtos,
                    totalAmount,
                    hasReview
            );
        });

        return PageUt.of(reservationSummaryDtoPage);
    }

    /**
     * 예약 총 금액을 계산
     */
    private int calculateTotalAmount(Reservation reservation, Post post, List<ReservationOption> options) {
        // 기간 일수 계산 (시작일과 종료일 포함)
        long days = ChronoUnit.DAYS.between(reservation.getReservationStartAt(), reservation.getReservationEndAt()) + 1;

        // 기본 가격 (Post 엔티티에서 직접 가져옴)
        int baseDeposit = post.getDeposit();
        int baseFee = post.getFee();

        // 옵션 가격 합산 (PostOption을 타고 들어가서 조회)
        int optionDepositSum = options.stream()
                .mapToInt(ro -> ro.getPostOption().getDeposit())
                .sum();
        int optionFeeSum = options.stream()
                .mapToInt(ro -> ro.getPostOption().getFee())
                .sum();

        int totalDeposit = baseDeposit + optionDepositSum;
        int totalDailyFee = baseFee + optionFeeSum;

        // 총 금액 = (총 보증금) + (총 일일 대여료 * 기간)
        return totalDeposit + (int) (totalDailyFee * days);
    }

    /**
     * Post 엔티티를 ReservationPostSummaryDto로 변환
     */
    private GuestReservationSummaryResBody.ReservationPostSummaryDto createPostSummaryDto(Post post) {
        AuthorDto authorDto =
                new AuthorDto(
                        post.getAuthor().getId(),
                        post.getAuthor().getNickname(),
                        post.getAuthor().getProfileImgUrl()
                );

        String thumbnailUrl = post.getImages().stream()
                .filter(img -> img.getIsPrimary() != null && img.getIsPrimary())
                .findFirst()
                .map(img -> img.getImageUrl())
                .orElse(null);

        return new GuestReservationSummaryResBody.ReservationPostSummaryDto(
                post.getId(),
                post.getTitle(),
                thumbnailUrl,
                authorDto
        );
    }

    public PagePayload<HostReservationSummaryResBody> getReceivedReservations(
            Long postId,
            Member member,
            Pageable pageable,
            ReservationStatus status,
            String keyword) {
        // postId로 게시글 조회 후, 해당 게시글의 author와 요청한 author가 일치하는지 확인
        Post post = postService.getById(postId);
        if (!post.getAuthor().getId().equals(member.getId())) {
            throw new ServiceException(HttpStatus.FORBIDDEN, "해당 게시글의 호스트가 아닙니다.");
        }
        Page<Reservation> reservationPage = reservationQueryRepository.findByPostWithFetch(post, status, pageable);

        Page<HostReservationSummaryResBody> reservationSummaryDtoPage = reservationPage.map(reservation -> {

            List<ReservationOption> options = reservation.getReservationOptions();

            // 총 금액 계산 (calculateTotalAmount 재사용)
            int totalAmount = calculateTotalAmount(reservation, post, options);

            // Option DTO 리스트 생성
            List<OptionDto> optionDtos = options.stream()
                    .map(ro -> new OptionDto(
                            ro.getPostOption().getId(),
                            ro.getPostOption().getName()
                    ))
                    .toList();

            // 최종 DTO 생성 (HostReservationSummaryResBody의 생성자 사용)
            return new HostReservationSummaryResBody(
                    reservation,
                    optionDtos,
                    totalAmount
            );
        });

        return PageUt.of(reservationSummaryDtoPage);
    }

    public ReservationDto getReservationDtoById(Long reservationId, Long memberId) {
        Reservation reservation = reservationQueryRepository.findByIdWithAll(reservationId)
                .orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "해당 예약을 찾을 수 없습니다."));

        // 권한 체크
        if (!reservation.getAuthor().getId().equals(memberId) &&
                !reservation.getPost().getAuthor().getId().equals(memberId)) {
            throw new ServiceException(HttpStatus.FORBIDDEN, "해당 예약에 대한 접근 권한이 없습니다.");
        }

        return convertToReservationDto(reservation);
    }

    public ReservationDto updateReservationStatus(Long reservationId, Long memberId, UpdateReservationStatusReqBody reqBody) {
        Reservation reservation = reservationQueryRepository.findByIdWithPostAndAuthor(reservationId)
                .orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "해당 예약을 찾을 수 없습니다."));

        // 역할 확인
        boolean isGuest = reservation.getAuthor().getId().equals(memberId);
        boolean isHost = reservation.getPost().getAuthor().getId().equals(memberId);

        if (!isGuest && !isHost) {
            throw new ServiceException(HttpStatus.FORBIDDEN, "해당 예약의 상태를 변경할 권한이 없습니다.");
        }

        // 상태 변경 권한 체크
        validateStatusTransitionPermission(reqBody.status(), isHost, isGuest);

        // 상태별 처리
        switch (reqBody.status()) {
            // 추가 데이터가 필요한 경우
            case REJECTED -> reservation.reject(reqBody.rejectReason());
            case CANCELLED -> reservation.cancel(reqBody.cancelReason());
            case CLAIMING -> reservation.claim(reqBody.claimReason());
            case SHIPPING -> reservation.startShipping(
                    reqBody.receiveCarrier(),
                    reqBody.receiveTrackingNumber()
            );
            case RETURNING -> reservation.startReturning(
                    reqBody.returnCarrier(),
                    reqBody.returnTrackingNumber()
            );
            // 반납 대기
            case PENDING_RETURN -> {
                // 대여 검수 -> 반납 대기 (검수 실패)
                if (reservation.getStatus() == ReservationStatus.INSPECTING_RENTAL) {
                    reservation.failRentalInspection(reqBody.cancelReason());
                } else {
                    // 대여 중 -> 반납 대기 (정상 반납 요청)
                    reservation.changeStatus(reqBody.status());
                }
            }

            // 단순 상태 전환 (명시적으로 나열)
            case PENDING_PAYMENT,
                 PENDING_PICKUP,
                 INSPECTING_RENTAL,
                 RENTING,
                 RETURN_COMPLETED,
                 INSPECTING_RETURN,
                 PENDING_REFUND,
                 REFUND_COMPLETED,
                 LOST_OR_UNRETURNED,
                 CLAIM_COMPLETED -> reservation.changeStatus(reqBody.status());

            // 지원하지 않는 상태
            default -> throw new ServiceException(HttpStatus.BAD_REQUEST, "지원하지 않는 상태 전환입니다.");
        }

        Reservation r = reservationRepository.save(reservation);

        // 상태 전환 로그 저장
        ReservationLog log = new ReservationLog(reservation.getStatus(), reservation, memberId);
        reservationLogRepository.save(log);

        return convertToReservationDto(r);
    }

    private void validateStatusTransitionPermission(ReservationStatus targetStatus, boolean isHost, boolean isGuest) {
        switch (targetStatus.getStatusSubject()) {
            case HOST -> {
                if (!isHost) {
                    throw new ServiceException(HttpStatus.FORBIDDEN,
                            targetStatus.getDescription() + " 권한이 없습니다. (호스트만 가능)");
                }
            }
            case GUEST -> {
                if (!isGuest) {
                    throw new ServiceException(HttpStatus.FORBIDDEN,
                            targetStatus.getDescription() + " 권한이 없습니다. (게스트만 가능)");
                }
            }
            case SYSTEM_OR_ANY -> {
                // 시스템 또는 누구나 가능 - 권한 체크 통과
            }
        }
    }

    public Reservation getByIdWithPostAndAuthor(Long reservationId) {
        return reservationQueryRepository.findByIdWithPostAndAuthor(reservationId)
                .orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "해당 예약을 찾을 수 없습니다."));
    }

    public ReservationDto updateReservation(Long reservationId, Long memberId, UpdateReservationReqBody reqBody) {
        Reservation reservation = reservationQueryRepository.findByIdWithAll(reservationId)
                .orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "해당 예약을 찾을 수 없습니다."));

        // 권한 체크: 예약 작성한 게스트만 수정 가능
        if (!reservation.getAuthor().getId().equals(memberId)) {
            throw new ServiceException(HttpStatus.FORBIDDEN, "해당 예약을 수정할 권한이 없습니다.");
        }

        // 수정 가능 상태인지 체크
        if (!reservation.isModifiable()) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "현재 상태에서는 예약을 수정할 수 없습니다.");
        }

        // 배송 정보 유효성 체크
        validateDeliveryInfo(reqBody.receiveMethod(), reqBody.receiveAddress1(), reqBody.receiveAddress2());

        // 기간 중복 체크 (자기 자신 제외)
        validateNoOverlappingReservation(
                reservation.getPost().getId(),
                reqBody.reservationStartAt(),
                reqBody.reservationEndAt(),
                reservationId  // 자기 자신의 ID 전달
        );

        // 전달 방식 유효성 체크
        validateDeliveryMethods(reservation.getPost(), reqBody.receiveMethod(), reqBody.returnMethod());

        // 선택된 PostOption 엔티티 조회 및 유효성 검증
        List<PostOption> selectedOptions = getOptionsByIds(reservation.getPost().getId(), reqBody.optionIds());

        // 예약 정보 업데이트
        reservation.updateDetails(
                reqBody.receiveMethod(),
                reqBody.receiveAddress1(),
                reqBody.receiveAddress2(),
                reqBody.returnMethod(),
                reqBody.reservationStartAt(),
                reqBody.reservationEndAt(),
                selectedOptions
        );

        Reservation r = reservationRepository.save(reservation);
        return convertToReservationDto(r);
    }

    // 배송 주소 입력 검증 메서드
    private void validateDeliveryInfo(ReservationDeliveryMethod method, String address1, String address2) {
        if (method == ReservationDeliveryMethod.DELIVERY) {
            // 택배 배송인 경우 주소 필수
            if (address1 == null || address1.isBlank()) {
                throw new ServiceException(HttpStatus.BAD_REQUEST, "택배 배송 시 주소는 필수입니다.");
            }
        } else if (method == ReservationDeliveryMethod.DIRECT) {
            // 직거래인 경우 주소 불필요
            if ((address1 != null && !address1.isBlank()) ||
                    (address2 != null && !address2.isBlank())) {
                throw new ServiceException(HttpStatus.BAD_REQUEST, "직거래 방식에서는 주소를 입력할 수 없습니다.");
            }
        }
    }

    // 중복 제거: DTO 변환 로직을 별도 메서드로 추출
    private ReservationDto convertToReservationDto(Reservation reservation) {
        Post post = reservation.getPost();
        List<ReservationOption> options = reservation.getReservationOptions();

        int totalAmount = calculateTotalAmount(reservation, post, options);

        List<OptionDto> optionDtos = options.stream()
                .map(ro -> new OptionDto(
                        ro.getPostOption().getId(),
                        ro.getPostOption().getName()
                ))
                .toList();

        // 1. 로그 전체 조회 (reservation 하나 기준이니까 N+1 아님)
        List<ReservationLog> logs = reservationLogRepository.findByReservation(reservation);

        // 2. authorId 한 번에 모으기
        Set<Long> authorIds = logs.stream()
                .map(ReservationLog::getAuthorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 3. authorIds로 Member 한 번에 조회
        Map<Long, String> authorNicknameMap = authorIds.isEmpty()
                ? Collections.emptyMap()
                : memberRepository.findAllById(authorIds)
                .stream()
                .collect(Collectors.toMap(
                        Member::getId,
                        Member::getNickname
                ));


        // 4. 로그 DTO로 변환하면서 authorName 매핑
        List<ReservationLogDto> logDtos = logs.stream()
                .map(log -> {
                    String authorNickname = authorNicknameMap.get(log.getAuthorId());
                    // 기본값 처리(탈퇴/없음 등) 하고 싶다면 여기서
                    if (authorNickname == null) {
                        authorNickname = "알 수 없는 사용자";
                    }
                    return new ReservationLogDto(log, authorNickname);
                })
                .toList();

        return new ReservationDto(
                reservation,
                optionDtos,
                logDtos,
                totalAmount
        );
    }
}