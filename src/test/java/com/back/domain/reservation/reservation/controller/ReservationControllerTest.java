package com.back.domain.reservation.reservation.controller;

import com.back.domain.reservation.reservation.common.ReservationDeliveryMethod;
import com.back.domain.reservation.reservation.entity.Reservation;
import com.back.domain.reservation.reservation.repository.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
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
    private MockMvc mockMvc;

    @Autowired
    private ReservationRepository reservationRepository;

    @Test
    @WithUserDetails("test@example.com")
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

        // 실제로 예약이 생성되었는지 확인
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
    @WithUserDetails("test@example.com")
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

//    @Test
//    void createReservation_shouldReturnUnauthorized_whenNotAuthenticated() throws Exception {
//        String jsonRequest = """
//                {
//                  "receiveMethod": "PICKUP",
//                  "receiveCarrier": "Carrier1",
//                  "receiveTrackingNumber": "TRACK123",
//                  "receiveAddress1": "123 Main St",
//                  "receiveAddress2": "Floor 2",
//                  "returnMethod": "DROP_OFF",
//                  "returnCarrier": "Carrier2",
//                  "returnTrackingNumber": "RETURN123",
//                  "reservationStartAt": "2025-11-12",
//                  "reservationEndAt": "2025-11-19"
//                }
//                """;
//
//        mockMvc.perform(post("/api/v1/reservations")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(jsonRequest))
//                .andExpect(status().isUnauthorized());
//
//        assertThat(reservationRepository.count()).isZero();
//    }
}