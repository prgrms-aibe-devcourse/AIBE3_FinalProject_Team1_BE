package com.back.domain.review.review.controller;

import com.back.domain.reservation.reservation.entity.Reservation;
import com.back.domain.reservation.reservation.repository.ReservationRepository;
import com.back.domain.review.review.repository.ReviewRepository;
import com.back.domain.review.review.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private Long reservationId;

    @BeforeEach
    void setUp() {
        // TestInitData에서 생성한 예약 데이터 가져오기
        Reservation reservation = reservationRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("TestInitData의 예약 데이터를 찾을 수 없습니다."));
        
        reservationId = reservation.getId();
    }

//    @Test
//    @WithUserDetails("test@example.com")
//    void write_shouldRegisterReviewSuccessfully() throws Exception {
//        String requestBody = """
//                {
//                    "equipmentScore": 5,
//                    "kindnessScore": 4,
//                    "responseTimeScore": 3,
//                    "comment": "Great service!"
//                }
//                """;
//
//        mockMvc.perform(post("/api/v1/reviews/" + reservationId)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(requestBody))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.resultCode").value("200-1"))
//                .andExpect(jsonPath("$.msg").value("리뷰가 작성되었습니다."));
//
//        // 실제 리뷰가 저장되었는지 확인
//        List<Review> reviews = reviewRepository.findAll();
//        assertThat(reviews).hasSize(1);
//
//        Review savedReview = reviews.get(0);
//
//        assertThat(savedReview.getEquipmentScore()).isEqualTo(5);
//        assertThat(savedReview.getKindnessScore()).isEqualTo(4);
//        assertThat(savedReview.getResponseTimeScore()).isEqualTo(3);
//        assertThat(savedReview.getComment()).isEqualTo("Great service!");
//    }
//
//    @Test
//    @WithUserDetails("test@example.com")
//    void write_shouldReturnBadRequestWhenValidationFails() throws Exception {
//        String requestBody = """
//                {
//                    "equipmentScore": 7,
//                    "kindnessScore": 4,
//                    "responseTimeScore": -1,
//                    "comment": ""
//                }
//                """;
//
//        mockMvc.perform(post("/api/v1/reviews/" + reservationId)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(requestBody))
//                .andExpect(status().isBadRequest());
//
//        assertThat(reviewRepository.count()).isZero();
//    }
//
//    @Test
//    @WithUserDetails("test@example.com")
//    void write_shouldReturnBadRequestWhenRequestBodyIsNull() throws Exception {
//        mockMvc.perform(post("/api/v1/reviews/" + reservationId)
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isBadRequest());
//
//        assertThat(reviewRepository.count()).isZero();
//    }
//
//    @Test
//    @WithUserDetails("test@example.com")
//    void write_shouldReturnBadRequestForInvalidReservationId() throws Exception {
//        String requestBody = """
//                {
//                    "equipmentScore": 5,
//                    "kindnessScore": 3,
//                    "responseTimeScore": 4,
//                    "comment": "Great!"
//                }
//                """;
//
//        mockMvc.perform(post("/api/v1/reviews/-1")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(requestBody))
//                .andExpect(status().isBadRequest());
//
//        assertThat(reviewRepository.count()).isZero();
//    }
//
//    @Test
//    @WithUserDetails("test@example.com")
//    void write_shouldReturnBadRequestWhenInputFieldsAreNull() throws Exception {
//        String requestBody = """
//                {
//                    "equipmentScore": null,
//                    "kindnessScore": null,
//                    "responseTimeScore": null,
//                    "comment": null
//                }
//                """;
//
//        mockMvc.perform(post("/api/v1/reviews/" + reservationId)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(requestBody))
//                .andExpect(status().isBadRequest());
//
//        assertThat(reviewRepository.count()).isZero();
//    }
}