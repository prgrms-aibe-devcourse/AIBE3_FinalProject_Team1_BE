package com.back.domain.review.review.controller;

import com.back.WithSecurityUser;
import com.back.domain.member.member.entity.Member;
import com.back.domain.post.post.common.ReceiveMethod;
import com.back.domain.post.post.common.ReturnMethod;
import com.back.domain.post.post.entity.Post;
import com.back.domain.reservation.reservation.common.ReservationStatus;
import com.back.domain.reservation.reservation.entity.Reservation;
import com.back.domain.review.review.entity.Review;
import com.back.domain.review.review.repository.ReviewRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReviewControllerTest {
    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReviewRepository reviewRepository;

    private Long reservationId;

    @BeforeEach
    void setUp() {
        Member host = Member.builder()
                .email("host@test.com")
                .password("password123")
                .name("호스트")
                .address1("서울시 강남구")
                .address2("테헤란로")
                .nickname("호스트닉네임")
                .phoneNumber("010-1234-5678")
                .build();

        Member guest = Member.builder()
                .email("test@example.com")
                .password("password123")
                .name("게스트")
                .address1("서울시 서초구")
                .address2("반포대로")
                .nickname("게스트닉네임")
                .phoneNumber("010-8765-4321")
                .build();

        Post post = Post.builder()
                .title("테스트 포스트")
                .content("테스트 내용")
                .fee(10000)
                .deposit(4)
                .author(host)
                .receiveMethod(ReceiveMethod.DELIVERY)  // 필수 필드 추가
                .returnMethod(ReturnMethod.DELIVERY)    // 필수 필드 추가
                .returnAddress1("서울시 강남구")        // 반납 주소 추가
                .returnAddress2("테헤란로")            // 반납 주소 추가
                .build();

        Reservation reservation = Reservation.builder()
                .author(guest)
                .status(ReservationStatus.RETURN_COMPLETED)
                .build();

        em.persist(host);
        em.persist(guest);
        em.persist(post);
        em.persist(reservation);

        em.flush();
        em.clear();

        reservationId = reservation.getId();
    }


    @Test
    @WithSecurityUser(
            id = 2L,  // guest의 ID
            email = "test@example.com",
            nickname = "게스트닉네임",
            roles = {"USER"}
    )
    void write_shouldRegisterReviewSuccessfully() throws Exception {
        String requestBody = """
                {
                    "equipmentScore": 5,
                    "kindnessScore": 4,
                    "responseTimeScore": 3,
                    "comment": "Great service!"
                }
                """;

        mockMvc.perform(post("/api/v1/reviews/" + reservationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("리뷰가 작성되었습니다."));

        // 실제 리뷰가 저장되었는지 확인
        List<Review> reviews = reviewRepository.findAll();
        assertThat(reviews).hasSize(1);

        Review savedReview = reviews.get(0);

        assertThat(savedReview.getEquipmentScore()).isEqualTo(5);
        assertThat(savedReview.getKindnessScore()).isEqualTo(4);
        assertThat(savedReview.getResponseTimeScore()).isEqualTo(3);
        assertThat(savedReview.getComment()).isEqualTo("Great service!");
    }

    @Test
    @WithSecurityUser(
            id = 2L,  // guest의 ID
            email = "test@example.com",
            nickname = "게스트닉네임",
            roles = {"USER"}
    )
    void write_shouldReturnBadRequestWhenValidationFails() throws Exception {
        String requestBody = """
                {
                    "equipmentScore": 7,
                    "kindnessScore": 4,
                    "responseTimeScore": -1,
                    "comment": ""
                }
                """;

        mockMvc.perform(post("/api/v1/reviews/" + reservationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        assertThat(reviewRepository.count()).isZero();
    }

    @Test
    @WithSecurityUser(
            id = 2L,  // guest의 ID
            email = "test@example.com",
            nickname = "게스트닉네임",
            roles = {"USER"}
    )
    void write_shouldReturnBadRequestWhenRequestBodyIsNull() throws Exception {
        mockMvc.perform(post("/api/v1/reviews/" + reservationId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        assertThat(reviewRepository.count()).isZero();
    }

    @Test
    @WithSecurityUser(
            id = 2L,  // guest의 ID
            email = "test@example.com",
            nickname = "게스트닉네임",
            roles = {"USER"}
    )
    void write_shouldReturnBadRequestForInvalidReservationId() throws Exception {
        String requestBody = """
                {
                    "equipmentScore": 5,
                    "kindnessScore": 3,
                    "responseTimeScore": 4,
                    "comment": "Great!"
                }
                """;

        mockMvc.perform(post("/api/v1/reviews/-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        assertThat(reviewRepository.count()).isZero();
    }

    @Test
    @WithSecurityUser(
            id = 2L,  // guest의 ID
            email = "test@example.com",
            nickname = "게스트닉네임",
            roles = {"USER"}
    )
    void write_shouldReturnBadRequestWhenInputFieldsAreNull() throws Exception {
        String requestBody = """
                {
                    "equipmentScore": null,
                    "kindnessScore": null,
                    "responseTimeScore": null,
                    "comment": null
                }
                """;

        mockMvc.perform(post("/api/v1/reviews/" + reservationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        assertThat(reviewRepository.count()).isZero();
    }
}