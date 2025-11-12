package com.back.domain.reservation.reservation.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.post.post.common.ReceiveMethod;
import com.back.domain.post.post.common.ReturnMethod;
import com.back.domain.post.post.entity.Post;
import com.back.domain.post.post.entity.PostOption;
import com.back.domain.post.post.service.PostService;
import com.back.domain.reservation.reservation.common.ReservationDeliveryMethod;
import com.back.domain.reservation.reservation.common.ReservationStatus;
import com.back.domain.reservation.reservation.dto.*;
import com.back.domain.reservation.reservation.entity.Reservation;
import com.back.domain.reservation.reservation.entity.ReservationLog;
import com.back.domain.reservation.reservation.entity.ReservationOption;
import com.back.domain.reservation.reservation.repository.ReservationLogRepository;
import com.back.domain.reservation.reservation.repository.ReservationRepository;
import com.back.global.exception.ServiceException;
import com.back.standard.util.page.PagePayload;
import com.back.standard.util.page.PageUt;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final ReservationLogRepository reservationLogRepository;
    private final PostService postService;

    public Long create(CreateReservationReqBody reqBody, Member author) {
        Post post = postService.getById(reqBody.postId());

        // 기간 중복 체크
        validateNoOverlappingReservation(
                post.getId(),
                reqBody.reservationStartAt(),
                reqBody.reservationEndAt(),
                null
        );

        // 같은 게스트의 중복 예약 체크 (게시글 ID 필요)
        validateNoDuplicateReservation(post.getId(), author.getId());

        // 전달 방식 유효성 체크
        validateDeliveryMethods(post, reqBody.receiveMethod(), reqBody.returnMethod());

        // 배송 정보 유효성 체크
        validateDeliveryInfo(reqBody.receiveMethod(), reqBody.receiveAddress1(), reqBody.receiveAddress2());

        // 선택된 PostOption 엔티티 조회 및 유효성 검증
        List<PostOption> selectedOptions = getOptionsByIds(post.getId(), reqBody.optionIds());

        // Reservation 엔티티 빌드
        Reservation reservation = Reservation.builder()
                .status(ReservationStatus.PENDING_APPROVAL)
                .receiveMethod(reqBody.receiveMethod())
                .receiveAddress1(reqBody.receiveAddress1())
                .receiveAddress2(reqBody.receiveAddress2())
                .returnMethod(reqBody.returnMethod())
                .reservationStartAt(reqBody.reservationStartAt())
                .reservationEndAt(reqBody.reservationEndAt())
                .author(author)
                .post(post)
                .build();

        // reservationOption 리스트 생성 및 설정
        if (!selectedOptions.isEmpty()) {
            List<ReservationOption> reservationOptions = selectedOptions.stream()
                    .map(postOption -> ReservationOption.builder()
                            // reservation 필드는 일단 null로 두고, 이후 setter로 설정
                            .postOption(postOption)
                            .reservation(reservation)
                            .build())
                    .toList();

            // Reservation의 리스트 필드에 추가 (addAllOptions 사용)
            reservation.addAllOptions(reservationOptions);
        }

        reservationRepository.save(reservation);
        return reservation.getId();
    }

    public long count() {
        return reservationRepository.count();
    }

    // 기간 중복 체크
    private void validateNoOverlappingReservation(Long postId, LocalDate start, LocalDate end, Long excludeId) {
        boolean hasOverlap = (excludeId == null)
                ? reservationRepository.existsOverlappingReservation(postId, start, end)
                : reservationRepository.existsOverlappingReservationExcludingSelf(postId, start, end, excludeId);

        if (hasOverlap) {
            throw new ServiceException("400-1", "해당 기간에 이미 예약이 있습니다.");
        }
    }

    // 같은 게스트의 중복 예약 체크
    private void validateNoDuplicateReservation(Long postId, Long authorId) {
        boolean exists = reservationRepository.existsActiveReservationByPostIdAndAuthorId(postId, authorId);
        if (exists) {
            throw new ServiceException("400-2", "이미 해당 게시글에 예약이 존재합니다.");
        }
    }

    private void validateDeliveryMethods(Post post, ReservationDeliveryMethod receiveMethod, ReservationDeliveryMethod returnMethod) {
        // 수령 방식 (Receive Method) 검증
        if (!isReceiveMethodAllowed(post.getReceiveMethod(), receiveMethod)) {
            throw new ServiceException(
                    "400-3",
                    "게시글에서 허용하는 수령 방식(%s)이 아닙니다.".formatted(post.getReceiveMethod().getDescription())
            );
        }

        // 반납 방식 (Return Method) 검증
        if (!isReturnMethodAllowed(post.getReturnMethod(), returnMethod)) {
            throw new ServiceException(
                    "400-4",
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
            throw new ServiceException("400-5", "선택된 옵션 중 유효하지 않은 옵션이 포함되어 있습니다."); // 400-3, 400-4와 충돌되지 않도록 코드 변경
        }

        // 해당 게시글의 옵션인지 검증
        boolean allBelongToPost = options.stream()
                .allMatch(option -> option.getPost().getId().equals(postId));

        if (!allBelongToPost) {
            throw new ServiceException("400-6", "선택된 옵션은 해당 게시글의 옵션이 아닙니다."); // 400-3, 400-4와 충돌되지 않도록 코드 변경
        }

        return options;
    }

    public PagePayload<GuestReservationSummaryResBody> getSentReservations(Member author, Pageable pageable, ReservationStatus status, String keyword) {
        // TODO: post의 제목을 keyword로 검색하도록 수정 필요
        // TODO: QueryDsl로 변경 예정
        Page<Reservation> reservationPage;
        if (status == null) {
            reservationPage = reservationRepository.findByAuthor(author, pageable);
        } else {
            reservationPage = reservationRepository.findByAuthorAndStatus(author, status, pageable);
        }

        // DTO 매핑 및 총 금액 계산 로직 수행
        Page<GuestReservationSummaryResBody> reservationSummaryDtoPage = reservationPage.map(reservation -> {

            Post post = reservation.getPost();
            List<ReservationOption> options = reservation.getReservationOptions(); // Lazy Loading 발생 가능

            // 총 금액 계산
            int totalAmount = calculateTotalAmount(reservation, post, options);

            // PostSummary DTO 생성
            GuestReservationSummaryResBody.ReservationPostSummaryDto postSummary = createPostSummaryDto(post);

            // Option DTO 리스트 생성 (ReservationOption -> PostOption을 통해 name, id 가져옴)
            List<OptionDto> optionDtos = options.stream()
                    .map(ro -> new OptionDto(
                            ro.getPostOption().getId(),
                            ro.getPostOption().getName() // PostOption 엔티티에서 가져옴
                    ))
                    .collect(Collectors.toList());

            // 최종 DTO 생성
            return new GuestReservationSummaryResBody(
                    reservation,
                    postSummary,
                    optionDtos,
                    totalAmount
            );
        });

        return PageUt.of(reservationSummaryDtoPage);
    }

    /**
     * 예약 총 금액을 계산 (PostOption을 Lazy Loading하여 접근)
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
            throw new ServiceException("403-1", "해당 게시글의 호스트가 아닙니다.");
        }

        Page<Reservation> reservationPage;
        if (status == null) {
            reservationPage = reservationRepository.findByPost(post, pageable);
        } else {
            reservationPage = reservationRepository.findByPostAndStatus(post, status, pageable);
        }

        Page<HostReservationSummaryResBody> reservationSummaryDtoPage = reservationPage.map(reservation -> {

            List<ReservationOption> options = reservation.getReservationOptions(); // Lazy Loading 발생 가능

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
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ServiceException("404-1", "해당 예약을 찾을 수 없습니다."));

        // 권한 체크
        if (!reservation.getAuthor().getId().equals(memberId) &&
                !reservation.getPost().getAuthor().getId().equals(memberId)) {
            throw new ServiceException("403-1", "해당 예약에 대한 접근 권한이 없습니다.");
        }

        Post post = reservation.getPost();
        List<ReservationOption> options = reservation.getReservationOptions();

        // 총 금액 계산 및 데이터 준비 (Service 책임)
        int totalAmount = calculateTotalAmount(reservation, post, options);

        // Option DTO 생성
        List<OptionDto> optionDtos = options.stream()
                .map(ro -> new OptionDto(
                        ro.getPostOption().getId(),
                        ro.getPostOption().getName()
                ))
                .toList();

        // ReservationLogDtoList 찾기
        List<ReservationLogDto> logDtos = reservationLogRepository.findByReservation(reservation).stream()
                .map(ReservationLogDto::new)
                .toList();

        return new ReservationDto(
                reservation,
                optionDtos,
                logDtos,
                totalAmount
        );
    }

    public void updateReservationStatus(Long reservationId, Long memberId, UpdateReservationStatusReqBody reqBody) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ServiceException("404-1", "해당 예약을 찾을 수 없습니다."));

        // 권한 체크
        boolean isGuest = reservation.getAuthor().getId().equals(memberId);
        boolean isHost = reservation.getPost().getAuthor().getId().equals(memberId);

        if (!isGuest && !isHost) {
            throw new ServiceException("403-1", "해당 예약의 상태를 변경할 권한이 없습니다.");
        }

        // 상태별 처리 및 권한 체크
        switch (reqBody.status()) {
            case PENDING_PAYMENT -> {
                // 승인 (승인 대기 -> 결제 대기)
                validateHostOnly(isHost, "승인");
                reservation.approve();
            }
            case REJECTED -> {
                // 거절 (승인 대기 -> 승인 거절)
                validateHostOnly(isHost, "거절");
                reservation.reject(reqBody.rejectReason());
            }
            case CANCELLED -> {
                // 취소 (여러 단계 -> 예약 취소)
                validateGuestOnly(isGuest, "취소");
                reservation.cancel(reqBody.cancelReason());
            }
            case PENDING_PICKUP -> {
                // 결제 완료 (결제 대기 -> 수령 대기)
                reservation.completePayment();
            }
            case SHIPPING -> {
                // 택배 배송 시작 (수령 대기 -> 배송 중)
                validateHostOnly(isHost, "배송 시작");
                reservation.startShipping(
                        reqBody.receiveCarrier(),
                        reqBody.receiveTrackingNumber()
                );
            }
            case INSPECTING_RENTAL -> {
                // 대여 검수 시작
                // (수령 대기 -> 대여 검수: 직거래)
                // (배송 중 -> 대여 검수: 배송 완료)
                reservation.startRentalInspection();
            }
            case RENTING -> {
                // 대여 시작 (대여 검수 -> 대여 중)
                validateGuestOnly(isGuest, "대여 시작");
                reservation.startRenting();
            }
            case PENDING_RETURN -> {
                // 반납 요청 (대여 중 -> 반납 대기)
                reservation.requestReturn();
            }
            case RETURNING -> {
                // 택배 반납 시작 (반납 대기 -> 반납 중)
                validateGuestOnly(isGuest, "반납 시작");
                reservation.startReturning(
                        reqBody.returnCarrier(),
                        reqBody.returnTrackingNumber()
                );
            }
            case RETURN_COMPLETED -> {
                // 반납 완료
                // (반납 대기 -> 반납 완료: 직거래)
                // (반납 중 -> 반납 완료: 택배 반납 완료)
                reservation.completeReturn();
            }
            case INSPECTING_RETURN -> {
                // 반납 검수 시작 (반납 완료 -> 반납 검수)
                validateHostOnly(isHost, "반납 검수");
                reservation.startReturnInspection();
            }
            case PENDING_REFUND -> {
                // 환급 예정 (반납 검수 -> 환급 예정)
                validateHostOnly(isHost, "환급 예정 처리");
                reservation.scheduleRefund();
            }
            case REFUND_COMPLETED -> {
                // 환급 완료 (환급 예정 -> 환급 완료)
                // 시스템 또는 호스트가 처리
                reservation.completeRefund();
            }
            case CLAIMING -> {
                // 청구 시작
                // (미반납/분실 -> 청구 진행)
                validateHostOnly(isHost, "청구 시작");
                reservation.startClaim();
            }
            case CLAIM_COMPLETED -> {
                // 청구 완료 (청구 진행 -> 청구 완료)
                reservation.completeClaim();
            }
            case LOST_OR_UNRETURNED -> {
                // 미반납/분실 처리 (대여 중 -> 미반납/분실)
                validateHostOnly(isHost, "미반납/분실 처리");
                reservation.markAsLost();
            }
            default -> throw new ServiceException("400-1", "지원하지 않는 상태 전환입니다.");
        }
        reservationRepository.save(reservation);

        // 상태 전환 로그 저장
        ReservationLog log = ReservationLog.builder()
                .reservation(reservation)
                .status(reservation.getStatus())
                .build();
        reservationLogRepository.save(log);
    }

    private void validateHostOnly(boolean isHost, String action) {
        if (!isHost) {
            throw new ServiceException("403-2",
                    String.format("호스트만 %s을(를) 수행할 수 있습니다.", action));
        }
    }

    private void validateGuestOnly(boolean isGuest, String action) {
        if (!isGuest) {
            throw new ServiceException("403-3",
                    String.format("게스트만 %s을(를) 수행할 수 있습니다.", action));
        }
    }

    public Reservation getById(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ServiceException("404-1", "해당 예약을 찾을 수 없습니다."));
    }

    public void updateReservation(Long reservationId, Long memberId, UpdateReservationReqBody reqBody) {
        Reservation reservation = reservationRepository.findByIdWithOptions(reservationId)
                .orElseThrow(() -> new ServiceException("404-1", "해당 예약을 찾을 수 없습니다."));

        // 권한 체크: 예약 작성한 게스트만 수정 가능
        if (!reservation.getAuthor().getId().equals(memberId)) {
            throw new ServiceException("403-1", "해당 예약을 수정할 권한이 없습니다.");
        }

        // 수정 가능 상태인지 체크
        if (!reservation.isModifiable()) {
            throw new ServiceException("400-1", "현재 상태에서는 예약을 수정할 수 없습니다.");
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

        reservationRepository.save(reservation);
    }

    // 배송 주소 입력 검증 메서드
    private void validateDeliveryInfo(ReservationDeliveryMethod method, String address1, String address2) {
        if (method == ReservationDeliveryMethod.DELIVERY) {
            // 택배 배송인 경우 주소 필수
            if (address1 == null || address1.isBlank()) {
                throw new ServiceException("400-20", "택배 배송 시 주소는 필수입니다.");
            }
        } else if (method == ReservationDeliveryMethod.DIRECT) {
            // 직거래인 경우 주소 불필요
            if ((address1 != null && !address1.isBlank()) ||
                    (address2 != null && !address2.isBlank())) {
                throw new ServiceException("400-21", "직거래 방식에서는 주소를 입력할 수 없습니다.");
            }
        }
    }
}
