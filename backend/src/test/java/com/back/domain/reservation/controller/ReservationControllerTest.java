package com.back.domain.reservation.controller;

import com.back.BaseContainerIntegrationTest;
import com.back.domain.reservation.repository.ReservationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
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
    @WithMockUser(value = "user1@example.com")
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
}