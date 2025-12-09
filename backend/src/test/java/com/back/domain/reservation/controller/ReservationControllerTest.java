package com.back.domain.reservation.controller;

import com.back.BaseContainerIntegrationTest;
import com.back.domain.reservation.common.ReservationDeliveryMethod;
import com.back.domain.reservation.common.ReservationStatus;
import com.back.domain.reservation.dto.UpdateReservationReqBody;
import com.back.domain.reservation.dto.UpdateReservationStatusReqBody;
import com.back.domain.reservation.repository.ReservationRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.*;

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
    @DisplayName("예약 등록 테스트")
    @WithUserDetails("user1@example.com")
    void createReservationTest() throws Exception {
        LocalDateTime reservationStartAt = LocalDateTime.now().plusDays(30);
        LocalDateTime reservationEndAt = LocalDateTime.now().plusDays(31);

        String startAtStr = reservationStartAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String endAtStr = reservationEndAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        String reqBody = """
            {
              "receiveMethod": "DIRECT",
              "receiveAddress1": null,
              "receiveAddress2": null,
              "returnMethod": "DIRECT",
              "reservationStartAt": "%s",
              "reservationEndAt": "%s",
              "postId": 5,
              "optionIds": null
            }
            """.formatted(startAtStr, endAtStr);

        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqBody))
                .andExpectAll(
                        status().isCreated(),
                        jsonPath("$.status").value(201),
                        jsonPath("$.msg").exists(),
                        jsonPath("$.data.id").exists(),
                        jsonPath("$.data.postId").value(5),
                        jsonPath("$.data.status").value("PENDING_APPROVAL"),
                        jsonPath("$.data.receiveMethod").value("DIRECT"),
                        jsonPath("$.data.returnMethod").value("DIRECT"),
                        jsonPath("$.data.reservationStartAt").value(startAtStr),
                        jsonPath("$.data.reservationEndAt").value(endAtStr),
                        jsonPath("$.data.option").isArray(),
                        jsonPath("$.data.logs").isArray(),
                        jsonPath("$.data.createdAt").exists(),
                        jsonPath("$.data.modifiedAt").exists()
                );
    }

    @Test
    @WithUserDetails("user1@example.com")
    @DisplayName("게스트가 보낸 예약 목록 조회 테스트")
    void getSentReservationsTest() throws Exception {
        // 멤버1은 reservation ID: 1,2,3 을 가지고 있음
        mockMvc.perform(get("/api/v1/reservations/sent")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.status").value(200),
                        jsonPath("$.data.content").isArray(),
                        jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(0))),
                        jsonPath("$.data.page").exists(),
                        jsonPath("$.data.page.page").isNumber(),
                        jsonPath("$.data.page.size").isNumber(),
                        jsonPath("$.data.page.totalElements").isNumber(),
                        jsonPath("$.data.page.totalPages").isNumber()
                );
    }

    @Test
    @WithUserDetails("user1@example.com") // 예약 목록을 보낸 게스트 사용자
    @DisplayName("게스트의 예약 상태별 개수 조회 테스트")
    void getSentReservationStatusCountsTest() throws Exception {
        mockMvc.perform(get("/api/v1/reservations/sent/status"))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.status").value(is(200)),
                        jsonPath("$.msg").value(containsString("게스트의 예약 상태별 개수입니다")),
                        jsonPath("$.data").exists(),
                        jsonPath("$.data.totalCount").isNumber(),
                        jsonPath("$.data.statusCounts").exists()
                );
    }

    @Test
    @WithUserDetails("user1@example.com") // DB 상의 1번 게시글(postId=1)의 작성자(author_id)의 이메일로 설정
    @DisplayName("호스트가 자신의 게시글에 대한 예약 목록 조회 테스트")
    void getReceivedReservationsTest_Success() throws Exception {
        Long postId = 1L; // 1번 게시글에 대한 예약 목록 조회

        mockMvc.perform(get("/api/v1/reservations/received/{postId}", postId)
                        .param("page", "0")
                        .param("size", "5"))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.status").value(200),
                        jsonPath("$.msg").value("%d번 게시글에 대한 예약 목록입니다.".formatted(postId)),
                        jsonPath("$.data").exists(),
                        jsonPath("$.data.content").isArray(),
                        jsonPath("$.data.content", org.hamcrest.Matchers.hasSize(greaterThanOrEqualTo(0))),
                        jsonPath("$.data.page").exists(),
                        jsonPath("$.data.page.size").isNumber(),
                        jsonPath("$.data.page.totalElements").isNumber(),
                        jsonPath("$.data.page.totalPages").isNumber()
                );
    }

    @Test
    @WithUserDetails("user2@example.com") // 1번 예약의 게스트(author_id=2)
    @DisplayName("게스트의 예약 상세 정보 조회 테스트")
    void getReservationDetailTest_GuestAccessSuccess() throws Exception {
        Long reservationId = 1L;

        mockMvc.perform(get("/api/v1/reservations/{id}", reservationId))
                .andExpectAll(
                        // 1. HTTP 상태 코드 검증
                        status().isOk(),
                        jsonPath("$.status").value(200),
                        jsonPath("$.msg").value("%d번 예약 상세 정보입니다.".formatted(reservationId)),
                        jsonPath("$.data.id").value(is(reservationId.intValue())),
                        jsonPath("$.data.status").value("RETURN_COMPLETED"),
                        jsonPath("$.data.author.nickname").value("chulsu"),
                        jsonPath("$.data.logs").isArray(),
                        jsonPath("$.data.totalAmount").isNumber()
                );
    }

    @Test
    @WithUserDetails("user2@example.com") // 7번 예약의 게스트(author_id=2)
    @DisplayName("게스트의 예약 내용 수정 테스트")
    void updateReservationTest() throws Exception {
        Long reservationId = 7L;

        // 1. 수정할 요청 본문(Request Body) 객체 생성
        UpdateReservationReqBody reqBody = new UpdateReservationReqBody(
                ReservationDeliveryMethod.DIRECT,
                null,
                null,
                ReservationDeliveryMethod.DIRECT,
                Collections.emptyList(),
                LocalDateTime.now().plusDays(60),
                LocalDateTime.now().plusDays(61)
        );
        String content = objectMapper.writeValueAsString(reqBody);

        mockMvc.perform(put("/api/v1/reservations/{id}", reservationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.status").value(is(200)),
                        jsonPath("$.msg").value(is("%d번 예약이 수정되었습니다.".formatted(reservationId))),
                        jsonPath("$.data.id").value(is(reservationId.intValue())),
                        jsonPath("$.data.receiveMethod").value(is("DIRECT")),
                        jsonPath("$.data.totalAmount").isNumber()
                );
    }

    @Test
    @WithUserDetails("user2@example.com") // 7번 예약의 게스트 (취소 요청 권한 보유자)
    @DisplayName("게스트의 예약 상태 업데이트 테스트 (PENDING_APPROVAL -> CANCELLED, 성공)")
    void updateReservationStatusTest_Cancel() throws Exception {
        Long reservationId = 7L;

        UpdateReservationStatusReqBody requestBody  = new UpdateReservationStatusReqBody(
                ReservationStatus.CANCELLED,
                "취소 사유",
                null,
                null,
                null,
                null,
                null,
                null
        );
        String content = objectMapper.writeValueAsString(requestBody);


        mockMvc.perform(patch("/api/v1/reservations/{id}/status", reservationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.status").value(is(200)),
                        jsonPath("$.msg").value(is("%d번 예약 상태가 업데이트 되었습니다.".formatted(reservationId))),
                        jsonPath("$.data.id").value(is(reservationId.intValue())),
                        jsonPath("$.data.status").value(is("CANCELLED")),
                        jsonPath("$.data.cancelReason").value(is("취소 사유")),
                        jsonPath("$.data.receiveCarrier").value(nullValue()),
                        jsonPath("$.data.rejectReason").value(nullValue())
                );
    }

    @Test
    @WithUserDetails("user1@example.com")
    @DisplayName("예약 승낙 시 동시성 체크 테스트")
    void concurrentApprovalTest() throws Exception {
        Long reservation1Id = 8L;
        Long reservation2Id = 9L;

        UpdateReservationStatusReqBody reqBody = new UpdateReservationStatusReqBody(
                ReservationStatus.PENDING_PAYMENT,
                null, null, null, null, null, null, null
        );
        String content = objectMapper.writeValueAsString(reqBody);

        int threadCount = 2;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger status1 = new AtomicInteger();
        AtomicInteger status2 = new AtomicInteger();

        Runnable rawTask1 = () -> {
            try {
                startLatch.await();
                var result = mockMvc.perform(
                                patch("/api/v1/reservations/{id}/status", reservation1Id)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(content)
                        )
                        .andReturn();
                status1.set(result.getResponse().getStatus());
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                doneLatch.countDown();
            }
        };

        Runnable rawTask2 = () -> {
            try {
                startLatch.await();
                var result = mockMvc.perform(
                                patch("/api/v1/reservations/{id}/status", reservation2Id)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(content)
                        )
                        .andReturn();
                status2.set(result.getResponse().getStatus());
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                doneLatch.countDown();
            }
        };

        Runnable task1 = new DelegatingSecurityContextRunnable(rawTask1, SecurityContextHolder.getContext());
        Runnable task2 = new DelegatingSecurityContextRunnable(rawTask2, SecurityContextHolder.getContext());

        new Thread(task1).start();
        new Thread(task2).start();

        // 동시에 출발
        startLatch.countDown();
        doneLatch.await();

        // === 응답 코드 검증 ===
        // 순서를 모르는 상태에서 하나는 200, 하나는 409 여야 함
        assertThat(List.of(status1.get(), status2.get()))
                .containsExactlyInAnyOrder(200, 409);

        // === 최종 DB 상태 검증 ===
        var r1 = reservationRepository.findById(reservation1Id).orElseThrow();
        var r2 = reservationRepository.findById(reservation2Id).orElseThrow();

        long approvedCount = Stream.of(r1, r2)
                .filter(r -> r.getStatus() == ReservationStatus.PENDING_PAYMENT)
                .count();

        assertThat(approvedCount)
                .as("동일 기간 중 승인된 예약은 정확히 하나여야 한다")
                .isEqualTo(1);
    }
}