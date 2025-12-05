package com.back.domain.reservation.controller;

import com.back.BaseContainerIntegrationTest;
import com.back.domain.reservation.repository.ReservationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Sql({
        "/sql/categories.sql",
        "/sql/regions.sql",
        "/sql/members.sql",
        "/sql/posts.sql",
        "/sql/reservations.sql",
        "/sql/reviews.sql",
        "/sql/notifications.sql"
})
@Sql(scripts = "/sql/clean-up.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class ReservationControllerTest extends BaseContainerIntegrationTest {

    @Autowired
    private ReservationRepository reservationRepository;

    @Test
    @WithUserDetails(value = "user1@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("예약 생성 테스트")
    void createReservationTest() throws Exception {
        LocalDateTime reservationStartAt = LocalDateTime.now().plusDays(1);
        LocalDateTime reservationEndAt = LocalDateTime.now().plusDays(8);

        String startAtStr = reservationStartAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String endAtStr = reservationEndAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        String reqBody = """
            {
              "receiveMethod": "DELIVERY",
              "receiveAddress1": "서울특별시 강남구",
              "receiveAddress2": "역삼동",
              "returnMethod": "DELIVERY",
              "reservationStartAt": "%s",
              "reservationEndAt": "%s",
              "postId": 4,
              "optionIds": null
            }
            """.formatted(startAtStr, endAtStr);

        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.msg").exists())
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.postId").value(4))
                .andExpect(jsonPath("$.data.status").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.data.receiveMethod").value("DELIVERY"))
                .andExpect(jsonPath("$.data.returnMethod").value("DELIVERY"))
                .andExpect(jsonPath("$.data.reservationStartAt").value(startAtStr))
                .andExpect(jsonPath("$.data.reservationEndAt").value(endAtStr))
                .andExpect(jsonPath("$.data.author.id").value(1))
                .andExpect(jsonPath("$.data.option").isArray())
                .andExpect(jsonPath("$.data.logs").isArray())
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.modifiedAt").exists());
    }

    @Test
    @WithUserDetails(value = "user1@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("사용자가 보낸 예약 목록 조회 테스트")
    void getSentReservationsTest() throws Exception {
        // 멤버1은 reservation ID: 1,2,3 을 가지고 있음
        mockMvc.perform(get("/api/v1/reservations/sent")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg", containsString("1"))) // 멤버 ID 1
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(0))))
                .andExpect(jsonPath("$.data.page").exists())
                .andExpect(jsonPath("$.data.size").exists())
                .andExpect(jsonPath("$.data.totalElements").exists())
                .andExpect(jsonPath("$.data.totalPages").exists());
    }

    @Test
    @WithUserDetails(value = "user2@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("게시글에 대한 받은 예약 목록 조회")
    void getReceivedReservationsTest() throws Exception {
        Long postId = 4L; // 멤버2가 작성한 포스트

        mockMvc.perform(get("/api/v1/reservations/received/{postId}", postId)
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg", containsString(postId.toString())))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.page").exists());
    }

    @Test
    @WithUserDetails(value = "user1@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("예약 상세 조회")
    void getReservationDetailTest() throws Exception {
        Long reservationId = 1L; // 멤버1이 작성한 예약

        mockMvc.perform(get("/api/v1/reservations/{id}", reservationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg", containsString(reservationId.toString())))
                .andExpect(jsonPath("$.data.id").value(reservationId))
                .andExpect(jsonPath("$.data.postId").value(4))
                .andExpect(jsonPath("$.data.status").value("RETURN_COMPLETED"))
                .andExpect(jsonPath("$.data.author.id").value(1))
                .andExpect(jsonPath("$.data.reservationStartAt").exists())
                .andExpect(jsonPath("$.data.reservationEndAt").exists());
    }

    @Test
    @WithUserDetails(value = "user3@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("예약 정보 수정 테스트")
    void updateReservationTest() throws Exception {
        Long reservationId = 7L; // 멤버3의 예약
        LocalDateTime newStartAt = LocalDateTime.now().plusDays(30);
        LocalDateTime newEndAt = LocalDateTime.now().plusDays(31);

        String startAtStr = newStartAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String endAtStr = newEndAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        String reqBody = """
            {
              "receiveMethod": "DIRECT",
              "receiveAddress1": "서울특별시 서초구",
              "receiveAddress2": "반포동",
              "returnMethod": "DIRECT",
              "reservationStartAt": "%s",
              "reservationEndAt": "%s",
              "optionIds": null
            }
            """.formatted(startAtStr, endAtStr);

        mockMvc.perform(put("/api/v1/reservations/{id}", reservationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg", containsString("수정")))
                .andExpect(jsonPath("$.data.id").value(reservationId))
                .andExpect(jsonPath("$.data.receiveMethod").value("DIRECT"))
                .andExpect(jsonPath("$.data.returnMethod").value("DIRECT"))
                .andExpect(jsonPath("$.data.reservationStartAt").value(startAtStr))
                .andExpect(jsonPath("$.data.reservationEndAt").value(endAtStr));
    }

    @Test
    @WithUserDetails(value = "user3@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("예약 상태 변경 테스트 - 취소")
    void updateReservationStatusToApprovedTest() throws Exception {
        Long reservationId = 7L; // 멤버3이 예약한 멤버1의 포스트

        String reqBody = """
            {
              "status": "CANCELLED",
              "cancelReason": "그냥",
              "rejectReason": null,
              "claimReason": null,
              "receiveCarrier": null,
              "receiveTrackingNumber": null,
              "returnCarrier": null,
              "returnTrackingNumber": null
            }
            """;

        mockMvc.perform(patch("/api/v1/reservations/{id}/status", reservationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg", containsString("업데이트")))
                .andExpect(jsonPath("$.data.id").value(reservationId));
    }



    @Test
    @WithUserDetails(value = "user1@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("보낸 예약 상태별 개수 조회 - 멤버1")
    void getSentReservationsStatusTest() throws Exception {
        mockMvc.perform(get("/api/v1/reservations/sent/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg", containsString("1"))) // 멤버 ID 1
                .andExpect(jsonPath("$.data").exists());
    }
}