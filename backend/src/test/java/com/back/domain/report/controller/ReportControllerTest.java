package com.back.domain.report.controller;

import com.back.BaseContainerIntegrationTest;
import com.back.domain.member.common.MemberRole;
import com.back.domain.member.entity.Member;
import com.back.domain.report.common.ReportType;
import com.back.domain.report.dto.ReportReqBody;
import com.back.domain.report.entity.Report;
import com.back.domain.report.repository.ReportRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Sql("/sql/report/report.sql")
class ReportControllerTest extends BaseContainerIntegrationTest {

    @Autowired EntityManager em;
    @Autowired ReportRepository reportRepository;

    @Test
    @DisplayName("POST 타입 신고 생성 성공")
    @WithUserDetails("test-1@email.com")
    void createReport_PostType_Success() throws Exception {
        // given
        long targetId = 2L;
        ReportReqBody reportReqBody = new ReportReqBody(
                ReportType.POST,
                targetId,
                "부적절한 게시글입니다"
        );

        // when & then
        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportReqBody))
               )
                .andExpectAll(
                        status().isCreated(),
                        jsonPath("$.data.id").isNumber(),
                        jsonPath("$.data.reportType").value("POST"),
                        jsonPath("$.data.targetId").value(targetId),
                        jsonPath("$.data.comment").value(reportReqBody.comment())
                )
                .andDo(print());

        clearContext();

        List<Report> reports = reportRepository.findAll();
        assertThat(reports).hasSize(1);
        assertThat(reports.getFirst().getTargetId()).isEqualTo(targetId);
    }

    @Test
    @DisplayName("MEMBER 타입 신고 생성 성공")
    @WithUserDetails("test-1@email.com")
    void createReport_MemberType_Success() throws Exception {
        // given
        long targetId = 2L;

        ReportReqBody reportReqBody = new ReportReqBody(
                ReportType.MEMBER,
                targetId,
                "부적절한 사용자입니다"
        );

        // when & then
        mockMvc.perform(post("/api/v1/reports")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(reportReqBody))
               )
               .andExpectAll(
                       status().isCreated(),
                       jsonPath("$.data.id").isNumber(),
                       jsonPath("$.data.reportType").value("MEMBER"),
                       jsonPath("$.data.targetId").value(targetId),
                       jsonPath("$.data.comment").value(reportReqBody.comment())
               )
               .andDo(print());

        clearContext();

        List<Report> reports = reportRepository.findAll();
        assertThat(reports).hasSize(1);
        assertThat(reports.getFirst().getTargetId()).isEqualTo(targetId);
    }

    @Test
    @DisplayName("REVIEW 타입 신고 생성 성공")
    @WithUserDetails("test-1@email.com")
    void createReport_ReviewType_Success() throws Exception {
        // given
        long targetId = 1L;
        ReportReqBody reportReqBody = new ReportReqBody(
                ReportType.REVIEW,
                targetId,
                "부적절한 리뷰입니다"
        );

        // when & then
        mockMvc.perform(post("/api/v1/reports")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(reportReqBody))
               )
               .andExpectAll(
                       status().isCreated(),
                       jsonPath("$.data.id").isNumber(),
                       jsonPath("$.data.reportType").value("REVIEW"),
                       jsonPath("$.data.targetId").value(targetId),
                       jsonPath("$.data.comment").value(reportReqBody.comment())
               )
               .andDo(print());

        clearContext();

        List<Report> reports = reportRepository.findAll();
        assertThat(reports).hasSize(1);
        assertThat(reports.getFirst().getTargetId()).isEqualTo(targetId);
    }

    private void clearContext() {
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("인증되지 않은 사용자의 신고 생성 실패")
    @WithAnonymousUser
    void createReport_Unauthorized() throws Exception {
        // given
        ReportReqBody reportReqBody = new ReportReqBody(
                ReportType.POST,
                100L,
                "Test comment"
        );

        // when & then
        mockMvc.perform(post("/api/v1/reports")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(reportReqBody)))
               .andExpectAll(
                       status().isUnauthorized(),
                       jsonPath("$.status").value(401)
               )
               .andDo(print());

        clearContext();
        assertThat(reportRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("존재하지 않는 대상에 대한 신고 실패")
    @WithUserDetails("test-1@email.com")
    void createReport_TargetNotFound() throws Exception {
        // given
        ReportReqBody reportReqBody = new ReportReqBody(
                ReportType.POST,
                999L,
                "존재하지 않는 게시글 신고"
        );

        // when & then
        mockMvc.perform(post("/api/v1/reports")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(reportReqBody))
               )
               .andExpectAll(
                       status().isNotFound(),
                       jsonPath("$.status").value(404),
                       jsonPath("$.msg").value("존재하지 않는 게시글입니다.")
               )
               .andDo(print());

        clearContext();
        assertThat(reportRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("중복 신고 실패")
    @WithUserDetails("test-1@email.com")
    void createReport_DuplicateReport() throws Exception {
        // given
        Member existingMember = em.find(Member.class, 1L);
        Member targetMember = new Member("member@email.com", "test1234", "test-member", MemberRole.USER);
        em.persist(targetMember);

        Report report = Report.createMemberType(targetMember.getId(), "이미 신고됨", existingMember);
        em.persist(report);

        ReportReqBody reportReqBody = new ReportReqBody(
                ReportType.MEMBER,
                targetMember.getId(),
                "중복 신고"
        );

        // when & then
        mockMvc.perform(post("/api/v1/reports")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(reportReqBody))
               )
               .andExpectAll(
                       status().isBadRequest(),
                       jsonPath("$.status").value(400),
                       jsonPath("$.msg").value("이미 신고한 대상입니다.")
               )
               .andDo(print());

        clearContext();
        assertThat(reportRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("본인 신고 실패")
    @WithUserDetails("test-1@email.com")
    void createReport_SelfReport() throws Exception {
        // given
        ReportReqBody reportReqBody = new ReportReqBody(
                ReportType.MEMBER,
                1L,
                "본인 신고"
        );

        // when & then
        mockMvc.perform(post("/api/v1/reports")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(reportReqBody))
               )
               .andExpectAll(
                       status().isBadRequest(),
                       jsonPath("$.status").value(400),
                       jsonPath("$.msg").value("본인은 신고할 수 없습니다.")
               )
               .andDo(print());

        clearContext();
        assertThat(reportRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("reportType만 null인 경우")
    @WithUserDetails("test-1@email.com")
    void createReport_NullReportType() throws Exception {
        // given
        ReportReqBody invalidReqBody = new ReportReqBody(
                null,  // reportType null
                100L,
                "Valid comment"
        );

        // when & then
        mockMvc.perform(post("/api/v1/reports")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(invalidReqBody))
               )
               .andExpectAll(
                       status().isBadRequest(),
                       jsonPath("$.status").value(400),
                       jsonPath("$.msg").value(containsString("reportType"))
               )
               .andDo(print());

        clearContext();
        assertThat(reportRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("targetId만 null인 경우")
    @WithUserDetails("test-1@email.com")
    void createReport_NullTargetId() throws Exception {
        // given
        ReportReqBody invalidReqBody = new ReportReqBody(
                ReportType.POST,
                null,
                "Valid comment"
        );

        // when & then
        mockMvc.perform(post("/api/v1/reports")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(invalidReqBody))
               )
               .andExpectAll(
                       status().isBadRequest(),
                       jsonPath("$.status").value(400),
                       jsonPath("$.msg").value(containsString("targetId"))
               )
               .andDo(print());

        clearContext();
        assertThat(reportRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("comment가 빈 문자열인 경우")
    @WithUserDetails("test-1@email.com")
    void createReport_BlankComment() throws Exception {
        // given
        ReportReqBody invalidReqBody = new ReportReqBody(
                ReportType.POST,
                100L,
                ""
        );

        // when & then
        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReqBody))
               )
               .andExpectAll(
                       status().isBadRequest(),
                       jsonPath("$.status").value(400),
                       jsonPath("$.msg").value(containsString("comment"))
               )
               .andDo(print());

        clearContext();
        assertThat(reportRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("comment가 공백만 있는 경우")
    @WithUserDetails("test-1@email.com")
    void createReport_WhitespaceComment() throws Exception {
        // given
        ReportReqBody invalidReqBody = new ReportReqBody(
                ReportType.POST,
                100L,
                "   "
        );

        // when & then
        mockMvc.perform(post("/api/v1/reports")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(invalidReqBody))
               )
               .andExpectAll(
                       status().isBadRequest(),
                       jsonPath("$.status").value(400),
                       jsonPath("$.msg").value(containsString("comment"))
               )
               .andDo(print());

        clearContext();
        assertThat(reportRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("잘못된 요청 본문으로 신고 생성 실패")
    @WithUserDetails("test-1@email.com")
    void createReport_InvalidRequestBody() throws Exception {
        // given
        ReportReqBody invalidReqBody = new ReportReqBody(
                null,
                null,
                ""
        );

        // when & then
        mockMvc.perform(post("/api/v1/reports")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(invalidReqBody))
               )
               .andExpectAll(
                       status().isBadRequest(),
                       jsonPath("$.status").value(400),
                       jsonPath("$.msg").exists(),
                       jsonPath("$.msg").value(containsString("comment")),
                       jsonPath("$.msg").value(containsString("reportType")),
                       jsonPath("$.msg").value(containsString("targetId")),
                       jsonPath("$.data").doesNotExist()
               )
               .andDo(print());

        clearContext();
        assertThat(reportRepository.count()).isEqualTo(0);
    }
}