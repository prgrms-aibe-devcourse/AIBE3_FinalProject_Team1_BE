
package com.back.domain.reservation.reservation.controller;

import com.back.WithSecurityUser;
import com.back.domain.member.member.dto.MemberJoinReqBody;
import com.back.domain.member.member.service.MemberService;
import com.back.domain.reservation.reservation.common.ReservationDeliveryMethod;
import com.back.domain.reservation.reservation.entity.Reservation;
import com.back.domain.reservation.reservation.repository.ReservationRepository;
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

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReservationControllerTest {
    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private MemberService memberService;

    @BeforeEach
    void setUp() {
        // 테스트용 회원이 없는 경우에만 생성
        if (!memberService.findByEmail("test@example.com").isPresent()) {
            MemberJoinReqBody joinReqBody = new MemberJoinReqBody(
                    "test@example.com",
                    "password123",
                    "John Doe",
                    "123 Main St",
                    "Apt 4B",
                    "TestUser",
                    "123-456-7890"
            );
            memberService.join(joinReqBody);
        }
    }

    @Test
    @WithSecurityUser(
            id = 1L,
            email = "test@example.com",
            nickname = "TestUser",
            roles = {"USER"}
    )
    void createReservation_shouldReturnSuccess_whenRequestIsValid() throws Exception {
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(7);

        String jsonRequest = """
                {
                  "receiveMethod": "OFFLINE",
                  "receiveCarrier": "Carrier1",
                  "receiveTrackingNumber": "TRACK123",
                  "receiveAddress1": "123 Main St",
                  "receiveAddress2": "Floor 2",
                  "returnMethod": "OFFLINE",
                  "returnCarrier": "Carrier2",
                  "returnTrackingNumber": "RETURN123",
                  "reservationStartAt": "%s",
                  "reservationEndAt": "%s"
                }
                """.formatted(startDate, endDate);

        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").exists());

        List<Reservation> reservations = reservationRepository.findAll();
        assertThat(reservations).hasSize(1);

        Reservation savedReservation = reservations.get(0);
        assertThat(savedReservation.getAuthor().getEmail()).isEqualTo("test@example.com");
        assertThat(savedReservation.getReceiveMethod()).isEqualTo(ReservationDeliveryMethod.OFFLINE);
        assertThat(savedReservation.getReceiveCarrier()).isEqualTo("Carrier1");
        assertThat(savedReservation.getReservationStartAt()).isEqualTo(startDate);
        assertThat(savedReservation.getReservationEndAt()).isEqualTo(endDate);
    }

    @Test
    @WithSecurityUser(
            id = 1L,
            email = "test@example.com",
            nickname = "TestUser",
            roles = {"USER"}
    )
    void createReservation_shouldReturnBadRequest_whenRequestIsInvalid() throws Exception {
        String invalidJsonRequest = """
                {
                  "receiveMethod": null,
                  "receiveCarrier": "Carrier1",
                  "receiveTrackingNumber": "TRACK123",
                  "receiveAddress1": "123 Main St",
                  "receiveAddress2": "Floor 2",
                  "returnMethod": "DROP_OFF",
                  "returnCarrier": "Carrier2",
                  "returnTrackingNumber": "RETURN123",
                  "reservationStartAt": null,
                  "reservationEndAt": null
                }
                """;

        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJsonRequest))
                .andExpect(status().isBadRequest());

        assertThat(reservationRepository.count()).isZero();
    }

    @Test
    void createReservation_shouldReturnUnauthorized_whenNotAuthenticated() throws Exception {
        String jsonRequest = """
                {
                  "receiveMethod": "PICKUP",
                  "receiveCarrier": "Carrier1",
                  "receiveTrackingNumber": "TRACK123",
                  "receiveAddress1": "123 Main St",
                  "receiveAddress2": "Floor 2",
                  "returnMethod": "DROP_OFF",
                  "returnCarrier": "Carrier2",
                  "returnTrackingNumber": "RETURN123",
                  "reservationStartAt": "2025-11-12",
                  "reservationEndAt": "2025-11-19"
                }
                """;

        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isUnauthorized());

        assertThat(reservationRepository.count()).isZero();
    }
}