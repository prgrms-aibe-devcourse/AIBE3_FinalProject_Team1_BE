package com.back.domain.review.review.controller;

import com.back.domain.member.entity.Member;
import com.back.domain.member.service.AuthTokenService;
import com.back.domain.member.service.RefreshTokenStore;
import com.back.domain.reservation.common.ReservationDeliveryMethod;
import com.back.domain.reservation.common.ReservationStatus;
import com.back.domain.reservation.entity.Reservation;
import com.back.domain.reservation.service.ReservationService;
import com.back.domain.review.dto.ReviewAuthorDto;
import com.back.domain.review.dto.ReviewDto;
import com.back.domain.review.dto.ReviewWriteReqBody;
import com.back.domain.review.entity.Review;
import com.back.domain.review.service.ReviewService;
import com.back.global.security.SecurityUser;
import com.back.global.web.CookieHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReviewControllerTest {

    @MockitoBean
    private ReviewService reviewService;

    @MockitoBean
    private ReservationService reservationService;

    @MockitoBean
    private CookieHelper cookieHelper;

    @MockitoBean
    private AuthTokenService authTokenService;

    @MockitoBean
    private RefreshTokenStore refreshTokenStore;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Review testReview;
    private SecurityUser testUser;
    private Reservation testReservation;

    @BeforeEach
    void setup() {
        // 테스트용 사용자 설정
        testUser = new SecurityUser(
            1L,
            "test@example.com",
            "password",
            "테스트닉네임",
            Collections.emptyList()
        );

        // 테스트용 예약 설정
        Member author = new Member(testUser.getUsername(), "password",
                "테스트", "01012345678",
                "서울시 강남구", "테헤란로 123", testUser.getNickname());

        testReservation = new Reservation(
                ReservationStatus.RETURN_COMPLETED,
                ReservationDeliveryMethod.DIRECT,  // 또는 DELIVERY
                null,  // receiveAddress1
                null,  // receiveAddress2
                ReservationDeliveryMethod.DIRECT,  // 또는 DELIVERY
                LocalDate.now().plusDays(1),  // reservationStartAt
                LocalDate.now().plusDays(3),  // reservationEndAt
                author,
                null  // Post 객체도 필요
        );

        // 테스트용 리뷰 설정
        testReview = Review.create(testReservation, new ReviewWriteReqBody(4, 5, 3, "좋은 서비스였습니다."));

        ReflectionTestUtils.setField(testReservation, "id", 1L);
        ReflectionTestUtils.setField(testReview, "id", 1L);

        // Mock accessToken 설정
        when(cookieHelper.getCookieValue("accessToken", ""))
            .thenReturn("mock-access-token");
        
        // accessToken 검증 모의 설정
        Map<String, Object> mockClaims = new HashMap<>();
        mockClaims.put("id", testUser.getId());
        mockClaims.put("email", testUser.getUsername());
        mockClaims.put("nickname", testUser.getNickname());
        mockClaims.put("role", "USER");
        mockClaims.put("authVersion", 1L);
        
        when(authTokenService.payload("mock-access-token"))
            .thenReturn(mockClaims);
        
        // authVersion 검증 모의 설정
        when(refreshTokenStore.getAuthVersion(testUser.getId()))
            .thenReturn(1L);
    }

    @Test
    void writeReview_Success() throws Exception {
        // given
        ReviewWriteReqBody reqBody = new ReviewWriteReqBody(
                4, 5, 3, "좋은 서비스였습니다."
        );

        String jsonRequest = objectMapper.writeValueAsString(reqBody);

        doNothing().when(reviewService)
                .writeReview(eq(1L), any(ReviewWriteReqBody.class), eq(testUser.getId()));

        // when & then
        mockMvc.perform(post("/api/v1/reviews/{reservationId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .cookie(new Cookie("accessToken", "mock-access-token")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.msg").value("리뷰가 작성되었습니다."))
                .andExpect(jsonPath("$.data").doesNotExist()); // Void이므로 data 없음

        verify(reviewService).writeReview(eq(1L), any(ReviewWriteReqBody.class), eq(testUser.getId()));
    }

    @Test
    @WithMockUser
    void writeReview_InvalidScore() throws Exception {
        // given
        String invalidJsonRequest = """
        {
            "equipmentScore": -1,
            "kindnessScore": 6,
            "responseTimeScore": 3,
            "comment": "테스트"
        }
        """;

        // when & then
        mockMvc.perform(post("/api/v1/reviews/{reservationId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJsonRequest)
                        .with(user(testUser)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.msg").exists()); // 에러 메시지 확인
    }

    @Test
    void writeReview_Unauthorized() throws Exception {
        // given
        when(cookieHelper.getCookieValue("accessToken", ""))
                .thenReturn("");
        when(cookieHelper.getCookieValue("refreshToken", ""))
                .thenReturn("");

        when(authTokenService.payload(anyString()))
                .thenReturn(null);

        String jsonRequest = """
        {
            "equipmentScore": 4,
            "kindnessScore": 5,
            "responseTimeScore": 3,
            "comment": "테스트"
        }
        """;

        // when & then
        mockMvc.perform(post("/api/v1/reviews/{reservationId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void getPostReviews_Success() throws Exception {
        // given
        ReviewDto reviewDto = new ReviewDto(
                1L,
                4,
                5,
                3,
                "좋은 서비스였습니다.",
                LocalDateTime.now(),
                new ReviewAuthorDto(1L, "테스트닉네임", "profile.jpg")
        );

        Page<ReviewDto> reviewPage = new PageImpl<>(
                List.of(reviewDto),
                PageRequest.of(0, 30),
                1
        );

        when(reviewService.getPostReviews(any(Pageable.class), eq(1L)))
                .thenReturn(reviewPage);

        // when & then
        mockMvc.perform(get("/api/v1/posts/{postId}/reviews", 1L)
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("성공"))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].equipmentScore").value(4))
                .andExpect(jsonPath("$.data.content[0].kindnessScore").value(5))
                .andExpect(jsonPath("$.data.content[0].responseTimeScore").value(3))
                .andExpect(jsonPath("$.data.content[0].comment").value("좋은 서비스였습니다."))
                .andExpect(jsonPath("$.data.content[0].author.id").value(1))
                .andExpect(jsonPath("$.data.content[0].author.nickname").value("테스트닉네임"))
                .andExpect(jsonPath("$.data.page.page").value(0))
                .andExpect(jsonPath("$.data.page.size").value(30))
                .andExpect(jsonPath("$.data.page.totalElements").value(1))
                .andExpect(jsonPath("$.data.page.totalPages").value(1))
                .andExpect(jsonPath("$.data.page.first").value(true))
                .andExpect(jsonPath("$.data.page.last").value(true))
                .andExpect(jsonPath("$.data.page.hasNext").value(false))
                .andExpect(jsonPath("$.data.page.hasPrevious").value(false));
    }

    @Test
    @WithMockUser
    void getMemberReviews_Success() throws Exception {
        // given
        ReviewDto reviewDto = new ReviewDto(
                1L,
                4,
                5,
                3,
                "좋은 서비스였습니다.",
                LocalDateTime.now(),
                new ReviewAuthorDto(1L, "테스트닉네임", "profile.jpg")
        );

        Page<ReviewDto> reviewPage = new PageImpl<>(
                List.of(reviewDto),
                PageRequest.of(0, 30),
                1
        );

        when(reviewService.getMemberReviews(any(Pageable.class), eq(1L)))
                .thenReturn(reviewPage);

        // when & then
        mockMvc.perform(get("/api/v1/members/{memberId}/reviews", 1L)
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("성공"))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].equipmentScore").value(4))
                .andExpect(jsonPath("$.data.content[0].kindnessScore").value(5))
                .andExpect(jsonPath("$.data.content[0].responseTimeScore").value(3))
                .andExpect(jsonPath("$.data.content[0].comment").value("좋은 서비스였습니다."))
                .andExpect(jsonPath("$.data.content[0].author.id").value(1))
                .andExpect(jsonPath("$.data.content[0].author.nickname").value("테스트닉네임"))
                .andExpect(jsonPath("$.data.page.page").value(0))
                .andExpect(jsonPath("$.data.page.size").value(30))
                .andExpect(jsonPath("$.data.page.totalElements").value(1))
                .andExpect(jsonPath("$.data.page.totalPages").value(1));
    }
}