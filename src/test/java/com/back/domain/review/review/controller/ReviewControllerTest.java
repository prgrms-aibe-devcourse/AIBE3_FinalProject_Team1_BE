package com.back.domain.review.review.controller;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.service.AuthTokenService;
import com.back.domain.member.member.service.RefreshTokenStore;
import com.back.domain.reservation.reservation.common.ReservationStatus;
import com.back.domain.reservation.reservation.entity.Reservation;
import com.back.domain.reservation.reservation.service.ReservationService;
import com.back.domain.review.review.dto.ReviewWriteReqBody;
import com.back.domain.review.review.entity.Review;
import com.back.domain.review.review.service.ReviewService;
import com.back.global.security.SecurityUser;
import com.back.global.web.CookieHelper;
import com.back.global.web.HeaderHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
    private HeaderHelper headerHelper;

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
        Member author = Member.builder()
            .email(testUser.getUsername())
            .password("password")
            .name("테스트")
            .nickname(testUser.getNickname())
            .phoneNumber("01012345678")
            .address1("테스트 주소1")
            .address2("테스트 주소2")
            .build();
        
        testReservation = Reservation.builder()
            .author(author)
            .status(ReservationStatus.RETURN_COMPLETED)
            .build();

        // 테스트용 리뷰 설정
        testReview = Review.builder()
            .reservation(testReservation)
            .equipmentScore(4)
            .kindnessScore(5)
            .responseTimeScore(3)
            .comment("좋은 서비스였습니다.")
            .build();

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
        .andExpect(content().string("리뷰가 작성되었습니다."));

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
            .andExpect(status().isBadRequest());
    }

    @Test
    void writeReview_Unauthorized() throws Exception {
        // given
        // 인증되지 않은 상태를 시뮬레이션하기 위해 모든 토큰 관련 설정을 비움
        when(cookieHelper.getCookieValue("accessToken", ""))
            .thenReturn("");
        when(cookieHelper.getCookieValue("refreshToken", ""))
            .thenReturn("");
        
        // 토큰 검증 실패 시뮬레이션
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

//    @Test
//    @WithMockUser
//    void getPostReviews_Success() throws Exception {
//        // given
//        Page<ReviewDto> reviewPage = new PageImpl<>(
//            List.of(new ReviewDto(1L, 4, 5, 3, "좋은 서비스였습니다.", null)),
//            PageRequest.of(0, 30),
//            1
//        );
//
//        when(reviewService.getPostReviews(any(Pageable.class), eq(1L)))
//            .thenReturn(reviewPage);
//
//        // when & then
//        mockMvc.perform(get("/api/v1/posts/{postId}/reviews", 1L)
//                .with(user(testUser)))
//            .andExpect(status().isOk())
//            .andExpect(jsonPath("$.content[0].id").value(1))
//            .andExpect(jsonPath("$.content[0].equipmentScore").value(4))
//            .andExpect(jsonPath("$.content[0].comment").value("좋은 서비스였습니다."));
//    }
//
//    @Test
//    @WithMockUser
//    void getMemberReviews_Success() throws Exception {
//        // given
//        Page<ReviewDto> reviewPage = new PageImpl<>(
//            List.of(new ReviewDto(1L, 4, 5, 3, "좋은 서비스였습니다.", null)),
//            PageRequest.of(0, 30),
//            1
//        );
//
//        when(reviewService.getMemberReviews(any(Pageable.class), eq(1L)))
//            .thenReturn(reviewPage);
//
//        // when & then
//        mockMvc.perform(get("/api/v1/members/{memberId}/reviews", 1L)
//                .with(user(testUser)))
//            .andExpect(status().isOk())
//            .andExpect(jsonPath("$.content[0].id").value(1))
//            .andExpect(jsonPath("$.content[0].equipmentScore").value(4))
//            .andExpect(jsonPath("$.content[0].comment").value("좋은 서비스였습니다."));
//    }
}