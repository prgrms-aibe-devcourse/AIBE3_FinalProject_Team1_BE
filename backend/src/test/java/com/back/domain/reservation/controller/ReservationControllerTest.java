package com.back.domain.reservation.controller;

import com.back.domain.member.entity.Member;
import com.back.domain.member.service.AuthTokenService;
import com.back.domain.member.service.MemberService;
import com.back.domain.member.service.RefreshTokenStore;
import com.back.domain.reservation.common.ReservationDeliveryMethod;
import com.back.domain.reservation.common.ReservationStatus;
import com.back.domain.reservation.dto.*;
import com.back.domain.reservation.service.ReservationService;
import com.back.global.exception.ServiceException;
import com.back.global.security.SecurityUser;
import com.back.global.web.CookieHelper;
import com.back.standard.util.page.PageMeta;
import com.back.standard.util.page.PagePayload;
import com.back.standard.util.page.SortOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservationService reservationService;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private CookieHelper cookieHelper;

    @MockitoBean
    private AuthTokenService authTokenService;

    @MockitoBean
    private RefreshTokenStore refreshTokenStore;

    @Autowired
    private ObjectMapper objectMapper;

    private SecurityUser testUser;
    private Member testMember;

    @BeforeEach
    void setup() {
        testUser = new SecurityUser(
                1L,
                "user@test.com",
                "password",
                "testUser",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        testMember = mock(Member.class);
        when(testMember.getId()).thenReturn(1L);
        when(testMember.getEmail()).thenReturn("user@test.com");
        when(testMember.getNickname()).thenReturn("testUser");

        // 인증 설정
        when(cookieHelper.getCookieValue("accessToken", ""))
                .thenReturn("mock-access-token");
        when(cookieHelper.getCookieValue("refreshToken", null))
                .thenReturn("mock-refresh-token");

        Map<String, Object> mockClaims = new HashMap<>();
        mockClaims.put("id", testUser.getId());
        mockClaims.put("email", testUser.getUsername());
        mockClaims.put("nickname", testUser.getNickname());
        mockClaims.put("role", "USER");
        mockClaims.put("authVersion", 1L);

        when(authTokenService.payload("mock-access-token"))
                .thenReturn(mockClaims);

        when(refreshTokenStore.getAuthVersion(testUser.getId()))
                .thenReturn(1L);

        when(memberService.getById(testUser.getId()))
                .thenReturn(testMember);
    }

    // ==================== 예약 생성 테스트 ====================

    @Test
    @DisplayName("예약 생성 성공")
    void createReservation_Success() throws Exception {
        // given
        CreateReservationReqBody reqBody = new CreateReservationReqBody(
                ReservationDeliveryMethod.DELIVERY,
                "서울시 강남구",
                "테헤란로 123",
                ReservationDeliveryMethod.DELIVERY,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(3),
                100L,
                List.of(1L, 2L)
        );

        ReservationDto reservationDto = new ReservationDto(
                1L,
                100L,
                new AuthorDto(1L, "testUser", "profile.jpg"),
                ReservationStatus.PENDING_APPROVAL,
                ReservationDeliveryMethod.DELIVERY,
                null, null,
                "서울시 강남구",
                "테헤란로 123",
                ReservationDeliveryMethod.DELIVERY,
                null, null, null, null, null,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(3),
                LocalDateTime.now(),
                LocalDateTime.now(),
                List.of(),
                List.of(),
                50000
        );

        when(reservationService.create(any(CreateReservationReqBody.class), eq(testMember)))
                .thenReturn(reservationDto);

        // when & then
        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBody))
                        .cookie(new Cookie("accessToken", "mock-access-token")))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.msg").value(containsString("예약이 생성되었습니다")))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.postId").value(100))
                .andExpect(jsonPath("$.data.status").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.data.receiveMethod").value("DELIVERY"))
                .andExpect(jsonPath("$.data.returnMethod").value("DELIVERY"))
                .andExpect(jsonPath("$.data.totalAmount").value(50000));

        verify(reservationService, times(1)).create(any(CreateReservationReqBody.class), eq(testMember));
    }

    @Test
    @DisplayName("예약 생성 실패 - 필수 필드 누락")
    void createReservation_MissingRequiredFields() throws Exception {
        // given
        CreateReservationReqBody invalidReqBody = new CreateReservationReqBody(
                null,  // receiveMethod null
                "서울시 강남구",
                "테헤란로 123",
                null,  // returnMethod null
                null,  // reservationStartAt null
                null,  // reservationEndAt null
                null,  // postId null
                List.of()
        );

        // when & then
        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReqBody))
                        .cookie(new Cookie("accessToken", "mock-access-token")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(reservationService);
    }

    @Test
    @DisplayName("예약 생성 실패 - 옵션 개수 초과")
    void createReservation_TooManyOptions() throws Exception {
        // given
        CreateReservationReqBody reqBody = new CreateReservationReqBody(
                ReservationDeliveryMethod.DELIVERY,
                "서울시 강남구",
                "테헤란로 123",
                ReservationDeliveryMethod.DELIVERY,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(3),
                100L,
                List.of(1L, 2L, 3L, 4L, 5L, 6L)  // 6개 (최대 5개)
        );

        // when & then
        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBody))
                        .cookie(new Cookie("accessToken", "mock-access-token")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(reservationService);
    }

    @Test
    @DisplayName("예약 생성 실패 - 존재하지 않는 게시글")
    void createReservation_PostNotFound() throws Exception {
        // given
        CreateReservationReqBody reqBody = new CreateReservationReqBody(
                ReservationDeliveryMethod.DELIVERY,
                "서울시 강남구",
                "테헤란로 123",
                ReservationDeliveryMethod.DELIVERY,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(3),
                999L,
                List.of()
        );

        when(reservationService.create(any(CreateReservationReqBody.class), eq(testMember)))
                .thenThrow(new ServiceException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));

        // when & then
        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBody))
                        .cookie(new Cookie("accessToken", "mock-access-token")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.msg").value("게시글을 찾을 수 없습니다."));
    }

    // ==================== 보낸 예약 목록 조회 테스트 ====================

    @Test
    @DisplayName("보낸 예약 목록 조회 성공 - 필터 없음")
    void getSentReservations_NoFilter_Success() throws Exception {
        // given
        List<GuestReservationSummaryResBody> reservations = List.of(
                new GuestReservationSummaryResBody(
                        1L,
                        new GuestReservationSummaryResBody.ReservationPostSummaryDto(
                                100L, "노트북 대여", "thumbnail.jpg",
                                new AuthorDto(2L, "host", "host.jpg")
                        ),
                        ReservationStatus.PENDING_APPROVAL,
                        ReservationDeliveryMethod.DELIVERY,
                        ReservationDeliveryMethod.DELIVERY,
                        null, null,
                        LocalDateTime.now().plusDays(1),
                        LocalDateTime.now().plusDays(3),
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        List.of(),
                        50000,
                        false
                )
        );

        PageMeta pageMeta = new PageMeta(0, 5, 1, 1, true, true, false, false,
                List.of(new SortOrder("id", "DESC")));
        PagePayload<GuestReservationSummaryResBody> pagePayload = new PagePayload<>(reservations, pageMeta);

        when(reservationService.getSentReservations(eq(testMember), any(), isNull(), isNull()))
                .thenReturn(pagePayload);

        // when & then
        mockMvc.perform(get("/api/v1/reservations/sent")
                        .cookie(new Cookie("accessToken", "mock-access-token")))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value(containsString("게스트가 등록한 예약 목록입니다")))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].post.postId").value(100))
                .andExpect(jsonPath("$.data.content[0].status").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.data.page.page").value(0))
                .andExpect(jsonPath("$.data.page.size").value(5))
                .andExpect(jsonPath("$.data.page.totalElements").value(1));
    }

    @Test
    @DisplayName("보낸 예약 목록 조회 성공 - 상태 필터링")
    void getSentReservations_WithStatusFilter_Success() throws Exception {
        // given
        List<GuestReservationSummaryResBody> reservations = List.of(
                new GuestReservationSummaryResBody(
                        1L,
                        new GuestReservationSummaryResBody.ReservationPostSummaryDto(
                                100L, "노트북 대여", "thumbnail.jpg",
                                new AuthorDto(2L, "host", "host.jpg")
                        ),
                        ReservationStatus.RENTING,
                        ReservationDeliveryMethod.DELIVERY,
                        ReservationDeliveryMethod.DELIVERY,
                        null, null,
                        LocalDateTime.now(),
                        LocalDateTime.now().plusDays(2),
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        List.of(),
                        50000,
                        false
                )
        );

        PageMeta pageMeta = new PageMeta(0, 5, 1, 1, true, true, false, false,
                List.of(new SortOrder("id", "DESC")));
        PagePayload<GuestReservationSummaryResBody> pagePayload = new PagePayload<>(reservations, pageMeta);

        when(reservationService.getSentReservations(eq(testMember), any(), eq(ReservationStatus.RENTING), isNull()))
                .thenReturn(pagePayload);

        // when & then
        mockMvc.perform(get("/api/v1/reservations/sent")
                        .param("status", "RENTING")
                        .cookie(new Cookie("accessToken", "mock-access-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].status").value("RENTING"));
    }

    @Test
    @DisplayName("보낸 예약 목록 조회 성공 - 키워드 검색")
    void getSentReservations_WithKeyword_Success() throws Exception {
        // given
        PageMeta pageMeta = new PageMeta(0, 5, 0, 0, true, true, false, false,
                List.of(new SortOrder("id", "DESC")));
        PagePayload<GuestReservationSummaryResBody> pagePayload = new PagePayload<>(List.of(), pageMeta);

        when(reservationService.getSentReservations(eq(testMember), any(), isNull(), eq("노트북")))
                .thenReturn(pagePayload);

        // when & then
        mockMvc.perform(get("/api/v1/reservations/sent")
                        .param("keyword", "노트북")
                        .cookie(new Cookie("accessToken", "mock-access-token")))
                .andExpect(status().isOk());

        verify(reservationService, times(1))
                .getSentReservations(eq(testMember), any(), isNull(), eq("노트북"));
    }

    @Test
    @DisplayName("보낸 예약 목록 조회 성공 - 페이징")
    void getSentReservations_WithPaging_Success() throws Exception {
        // given
        PageMeta pageMeta = new PageMeta(1, 5, 10, 2, false, false, true, true,
                List.of(new SortOrder("id", "DESC")));
        PagePayload<GuestReservationSummaryResBody> pagePayload = new PagePayload<>(List.of(), pageMeta);

        when(reservationService.getSentReservations(eq(testMember), any(), isNull(), isNull()))
                .thenReturn(pagePayload);

        // when & then
        mockMvc.perform(get("/api/v1/reservations/sent")
                        .param("page", "1")
                        .param("size", "5")
                        .cookie(new Cookie("accessToken", "mock-access-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page.page").value(1))
                .andExpect(jsonPath("$.data.page.size").value(5))
                .andExpect(jsonPath("$.data.page.totalElements").value(10))
                .andExpect(jsonPath("$.data.page.totalPages").value(2));
    }

    // ==================== 받은 예약 목록 조회 테스트 ====================

    @Test
    @DisplayName("받은 예약 목록 조회 성공")
    void getReceivedReservations_Success() throws Exception {
        // given
        Long postId = 100L;
        List<HostReservationSummaryResBody> reservations = List.of(
                new HostReservationSummaryResBody(
                        1L, postId,
                        new AuthorDto(1L, "testUser", "profile.jpg"),
                        ReservationStatus.PENDING_APPROVAL,
                        ReservationDeliveryMethod.DELIVERY,
                        ReservationDeliveryMethod.DELIVERY,
                        null, null,
                        LocalDateTime.now().plusDays(1),
                        LocalDateTime.now().plusDays(3),
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        List.of(),
                        50000
                )
        );

        PageMeta pageMeta = new PageMeta(0, 5, 1, 1, true, true, false, false,
                List.of(new SortOrder("id", "DESC")));
        PagePayload<HostReservationSummaryResBody> pagePayload = new PagePayload<>(reservations, pageMeta);

        when(reservationService.getReceivedReservations(eq(postId), eq(testMember), any(), isNull(), isNull()))
                .thenReturn(pagePayload);

        // when & then
        mockMvc.perform(get("/api/v1/reservations/received/{postId}", postId)
                        .cookie(new Cookie("accessToken", "mock-access-token")))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value(containsString("게시글에 대한 예약 목록입니다")))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].postId").value(postId));
    }

    @Test
    @DisplayName("받은 예약 목록 조회 성공 - 상태 필터링")
    void getReceivedReservations_WithStatusFilter_Success() throws Exception {
        // given
        Long postId = 100L;
        PageMeta pageMeta = new PageMeta(0, 5, 0, 0, true, true, false, false,
                List.of(new SortOrder("id", "DESC")));
        PagePayload<HostReservationSummaryResBody> pagePayload = new PagePayload<>(List.of(), pageMeta);

        when(reservationService.getReceivedReservations(eq(postId), eq(testMember), any(),
                eq(ReservationStatus.RENTING), isNull()))
                .thenReturn(pagePayload);

        // when & then
        mockMvc.perform(get("/api/v1/reservations/received/{postId}", postId)
                        .param("status", "RENTING")
                        .cookie(new Cookie("accessToken", "mock-access-token")))
                .andExpect(status().isOk());

        verify(reservationService, times(1))
                .getReceivedReservations(eq(postId), eq(testMember), any(), eq(ReservationStatus.RENTING), isNull());
    }

    // ==================== 예약 상세 조회 테스트 ====================

    @Test
    @DisplayName("예약 상세 조회 성공")
    void getReservationDetail_Success() throws Exception {
        // given
        Long reservationId = 1L;
        ReservationDto reservationDto = new ReservationDto(
                reservationId,
                100L,
                new AuthorDto(1L, "testUser", "profile.jpg"),
                ReservationStatus.RENTING,
                ReservationDeliveryMethod.DELIVERY,
                "CJ대한통운", "1234567890",
                "서울시 강남구", "테헤란로 123",
                ReservationDeliveryMethod.DELIVERY,
                null, null, null, null, null,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now(),
                LocalDateTime.now(),
                List.of(),
                List.of(new ReservationLogDto(1L, ReservationStatus.PENDING_APPROVAL, LocalDateTime.now(), "테스트 유저")),
                50000
        );

        when(reservationService.getReservationDtoById(eq(reservationId), eq(testUser.getId())))
                .thenReturn(reservationDto);

        // when & then
        mockMvc.perform(get("/api/v1/reservations/{id}", reservationId)
                        .cookie(new Cookie("accessToken", "mock-access-token")))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value(containsString("예약 상세 정보입니다")))
                .andExpect(jsonPath("$.data.id").value(reservationId))
                .andExpect(jsonPath("$.data.postId").value(100))
                .andExpect(jsonPath("$.data.status").value("RENTING"))
                .andExpect(jsonPath("$.data.receiveCarrier").value("CJ대한통운"))
                .andExpect(jsonPath("$.data.receiveTrackingNumber").value("1234567890"))
                .andExpect(jsonPath("$.data.logs").isArray());
    }

    @Test
    @DisplayName("예약 상세 조회 실패 - 존재하지 않는 예약")
    void getReservationDetail_NotFound() throws Exception {
        // given
        Long reservationId = 999L;

        when(reservationService.getReservationDtoById(eq(reservationId), eq(testUser.getId())))
                .thenThrow(new ServiceException(HttpStatus.NOT_FOUND, "예약을 찾을 수 없습니다."));

        // when & then
        mockMvc.perform(get("/api/v1/reservations/{id}", reservationId)
                        .cookie(new Cookie("accessToken", "mock-access-token")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.msg").value("예약을 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("예약 상세 조회 실패 - 권한 없음")
    void getReservationDetail_Forbidden() throws Exception {
        // given
        Long reservationId = 1L;

        when(reservationService.getReservationDtoById(eq(reservationId), eq(testUser.getId())))
                .thenThrow(new ServiceException(HttpStatus.FORBIDDEN, "권한이 없습니다."));

        // when & then
        mockMvc.perform(get("/api/v1/reservations/{id}", reservationId)
                        .cookie(new Cookie("accessToken", "mock-access-token")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.msg").value("권한이 없습니다."));
    }

    // ==================== 예약 상태 업데이트 테스트 ====================

    @Test
    @DisplayName("예약 상태 업데이트 성공")
    void updateReservationStatus_Success() throws Exception {
        // given
        Long reservationId = 1L;
        UpdateReservationStatusReqBody reqBody = new UpdateReservationStatusReqBody(
                ReservationStatus.PENDING_PAYMENT,
                null,   // cancelReason
                null,   // rejectReason
                null,   // receiveCarrier
                null,   // claimReason
                null,   // receiveTrackingNumber
                null,   // returnCarrier
                null    // returnTrackingNumber
        );

        ReservationDto updatedDto = new ReservationDto(
                reservationId, 100L,
                new AuthorDto(1L, "testUser", "profile.jpg"),
                ReservationStatus.PENDING_PAYMENT,
                ReservationDeliveryMethod.DELIVERY,
                null, null, "서울시 강남구", "테헤란로 123",
                ReservationDeliveryMethod.DELIVERY,
                null, null, null, null, null,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(3),
                LocalDateTime.now(),
                LocalDateTime.now(),
                List.of(), List.of(), 50000
        );

        when(reservationService.updateReservationStatus(eq(reservationId), eq(testUser.getId()), any()))
                .thenReturn(updatedDto);

        // when & then
        mockMvc.perform(patch("/api/v1/reservations/{id}/status", reservationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBody))
                        .cookie(new Cookie("accessToken", "mock-access-token")))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value(containsString("예약 상태가 업데이트 되었습니다")))
                .andExpect(jsonPath("$.data.id").value(reservationId))
                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"));

        verify(reservationService, times(1))
                .updateReservationStatus(eq(reservationId), eq(testUser.getId()), any());
    }

    @Test
    @DisplayName("예약 상태 업데이트 실패 - 잘못된 상태 전환")
    void updateReservationStatus_InvalidTransition() throws Exception {
        // given
        Long reservationId = 1L;
        UpdateReservationStatusReqBody reqBody = new UpdateReservationStatusReqBody(
                ReservationStatus.REFUND_COMPLETED,
                "단순 변심",   // cancelReason (예시)
                null,          // rejectReason
                null,          // claimReason
                null, null,    // receiveCarrier, receiveTrackingNumber
                null, null     // returnCarrier, returnTrackingNumber
        );
        when(reservationService.updateReservationStatus(eq(reservationId), eq(testUser.getId()), any()))
                .thenThrow(new ServiceException(HttpStatus.BAD_REQUEST, "잘못된 상태 전환입니다."));

        // when & then
        mockMvc.perform(patch("/api/v1/reservations/{id}/status", reservationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBody))
                        .cookie(new Cookie("accessToken", "mock-access-token")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.msg").value("잘못된 상태 전환입니다."));
    }

    // ==================== 예약 수정 테스트 ====================

    @Test
    @DisplayName("예약 수정 성공")
    void updateReservation_Success() throws Exception {
        // given
        Long reservationId = 1L;
        UpdateReservationReqBody reqBody = new UpdateReservationReqBody(
                ReservationDeliveryMethod.DIRECT,
                null, null,
                ReservationDeliveryMethod.DIRECT,
                List.of(3L, 4L),
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(4)
        );

        ReservationDto updatedDto = new ReservationDto(
                reservationId, 100L,
                new AuthorDto(1L, "testUser", "profile.jpg"),
                ReservationStatus.PENDING_APPROVAL,
                ReservationDeliveryMethod.DIRECT,
                null, null, null, null,
                ReservationDeliveryMethod.DIRECT,
                null, null, null, null, null,
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(4),
                LocalDateTime.now(),
                LocalDateTime.now(),
                List.of(), List.of(), 60000
        );

        when(reservationService.updateReservation(eq(reservationId), eq(testUser.getId()), any()))
                .thenReturn(updatedDto);

        // when & then
        mockMvc.perform(put("/api/v1/reservations/{id}", reservationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBody))
                        .cookie(new Cookie("accessToken", "mock-access-token")))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value(containsString("예약이 수정되었습니다")))
                .andExpect(jsonPath("$.data.id").value(reservationId))
                .andExpect(jsonPath("$.data.receiveMethod").value("DIRECT"))
                .andExpect(jsonPath("$.data.returnMethod").value("DIRECT"));

        verify(reservationService, times(1))
                .updateReservation(eq(reservationId), eq(testUser.getId()), any());
    }

    @Test
    @DisplayName("예약 수정 실패 - 수정 불가능한 상태")
    void updateReservation_InvalidStatus() throws Exception {
        // given
        Long reservationId = 1L;
        UpdateReservationReqBody reqBody = new UpdateReservationReqBody(
                ReservationDeliveryMethod.DELIVERY,
                "서울시 서초구", "서초대로 456",
                ReservationDeliveryMethod.DELIVERY,
                List.of(),
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(4)
        );

        when(reservationService.updateReservation(eq(reservationId), eq(testUser.getId()), any()))
                .thenThrow(new ServiceException(HttpStatus.BAD_REQUEST, "수정할 수 없는 상태입니다."));

        // when & then
        mockMvc.perform(put("/api/v1/reservations/{id}", reservationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBody))
                        .cookie(new Cookie("accessToken", "mock-access-token")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.msg").value("수정할 수 없는 상태입니다."));
    }

    @Test
    @DisplayName("예약 수정 실패 - 필수 필드 누락")
    void updateReservation_MissingRequiredFields() throws Exception {
        // given
        Long reservationId = 1L;
        UpdateReservationReqBody invalidReqBody = new UpdateReservationReqBody(
                null,  // receiveMethod null
                null, null,
                null,  // returnMethod null
                List.of(),
                null,  // reservationStartAt null
                null   // reservationEndAt null
        );

        // when & then
        mockMvc.perform(put("/api/v1/reservations/{id}", reservationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReqBody))
                        .cookie(new Cookie("accessToken", "mock-access-token")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(reservationService);
    }

    // ==================== 인증 테스트 ====================

    @Test
    @DisplayName("인증되지 않은 사용자 - 예약 생성 실패")
    void createReservation_Unauthorized() throws Exception {
        // given
        when(cookieHelper.getCookieValue("accessToken", "")).thenReturn("");
        when(cookieHelper.getCookieValue("refreshToken", null)).thenReturn(null);
        when(authTokenService.payload("")).thenReturn(null);

        CreateReservationReqBody reqBody = new CreateReservationReqBody(
                ReservationDeliveryMethod.DELIVERY,
                "서울시 강남구", "테헤란로 123",
                ReservationDeliveryMethod.DELIVERY,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(3),
                100L, List.of()
        );

        // when & then
        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBody)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        verifyNoInteractions(reservationService);
    }
}