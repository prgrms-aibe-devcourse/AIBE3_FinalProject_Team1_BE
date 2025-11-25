package com.back.domain.report.controller;

import com.back.domain.member.service.AuthTokenService;
import com.back.domain.member.service.RefreshTokenStore;
import com.back.domain.report.common.ReportType;
import com.back.domain.report.dto.ReportReqBody;
import com.back.domain.report.dto.ReportResBody;
import com.back.domain.report.service.ReportService;
import com.back.global.exception.ServiceException;
import com.back.global.security.SecurityUser;
import com.back.global.web.CookieHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private CookieHelper cookieHelper;

    @MockitoBean
    private AuthTokenService authTokenService;

    @MockitoBean
    private RefreshTokenStore refreshTokenStore;

    @Autowired
    private ObjectMapper objectMapper;

    private SecurityUser testUser;

    @BeforeEach
    void setup() {
        testUser = new SecurityUser(
                1L,
                "user@test.com",
                "password",
                "testUser",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        when(cookieHelper.getCookieValue("accessToken", ""))
                .thenReturn("mock-access-token");

        Map<String, Object> mockClaims = new HashMap<>();
        mockClaims.put("id", testUser.getId());
        mockClaims.put("email", testUser.getUsername());
        mockClaims.put("nickname", testUser.getNickname());
        mockClaims.put("role", "USER");
        mockClaims.put("authVersion", 1L);

        when(authTokenService.payload("mock-access-token"))
                .thenReturn(mockClaims);

        when(refreshTokenStore.getAuthVersion(testUser.getId()))
                .thenReturn(1L);
    }

    @Test
    @DisplayName("신고 생성 성공")
    void createReport_Success() throws Exception {
        // given
        ReportReqBody reportReqBody = new ReportReqBody(
                ReportType.POST,
                100L,
                "This is a test comment"
        );

        ReportResBody reportResBody = new ReportResBody(
                1L,
                ReportType.POST,
                100L,
                "This is a test comment",
                testUser.getId(),
                LocalDateTime.now()
        );

        when(reportService.postReport(any(ReportReqBody.class), eq(testUser.getId())))
                .thenReturn(reportResBody);

        // when & then
        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportReqBody))
                        .cookie(new Cookie("accessToken", "mock-access-token")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.msg").value("CREATED"))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.reportType").value("POST"))
                .andExpect(jsonPath("$.data.targetId").value(100))
                .andExpect(jsonPath("$.data.comment").value("This is a test comment"))
                .andExpect(jsonPath("$.data.authorId").value(testUser.getId()));

        verify(reportService, times(1)).postReport(any(ReportReqBody.class), eq(testUser.getId()));
    }

    @Test
    @DisplayName("잘못된 요청 본문으로 신고 생성 실패")
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
                        .cookie(new Cookie("accessToken", "mock-access-token")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.msg").exists())
                .andExpect(jsonPath("$.msg").value(containsString("comment")))
                .andExpect(jsonPath("$.msg").value(containsString("reportType")))
                .andExpect(jsonPath("$.msg").value(containsString("targetId")))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(reportService, never()).postReport(any(ReportReqBody.class), anyLong());
    }

    @Test
    @DisplayName("POST 타입 신고 생성 성공")
    void createReport_PostType_Success() throws Exception {
        // given
        ReportReqBody reportReqBody = new ReportReqBody(
                ReportType.POST,
                200L,
                "부적절한 게시글입니다"
        );

        ReportResBody reportResBody = new ReportResBody(
                2L,
                ReportType.POST,
                200L,
                "부적절한 게시글입니다",
                testUser.getId(),
                LocalDateTime.now()
        );

        when(reportService.postReport(any(ReportReqBody.class), eq(testUser.getId())))
                .thenReturn(reportResBody);

        // when & then
        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportReqBody))
                        .cookie(new Cookie("accessToken", "mock-access-token")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.reportType").value("POST"))
                .andExpect(jsonPath("$.data.targetId").value(200));
    }

    @Test
    @DisplayName("MEMBER 타입 신고 생성 성공")
    void createReport_MemberType_Success() throws Exception {
        // given
        ReportReqBody reportReqBody = new ReportReqBody(
                ReportType.MEMBER,
                300L,
                "부적절한 사용자입니다"
        );

        ReportResBody reportResBody = new ReportResBody(
                3L,
                ReportType.MEMBER,
                300L,
                "부적절한 사용자입니다",
                testUser.getId(),
                LocalDateTime.now()
        );

        when(reportService.postReport(any(ReportReqBody.class), eq(testUser.getId())))
                .thenReturn(reportResBody);

        // when & then
        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportReqBody))
                        .cookie(new Cookie("accessToken", "mock-access-token")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.reportType").value("MEMBER"))
                .andExpect(jsonPath("$.data.targetId").value(300));
    }

    @Test
    @DisplayName("REVIEW 타입 신고 생성 성공")
    void createReport_ReviewType_Success() throws Exception {
        // given
        ReportReqBody reportReqBody = new ReportReqBody(
                ReportType.REVIEW,
                400L,
                "부적절한 리뷰입니다"
        );

        ReportResBody reportResBody = new ReportResBody(
                4L,
                ReportType.REVIEW,
                400L,
                "부적절한 리뷰입니다",
                testUser.getId(),
                LocalDateTime.now()
        );

        when(reportService.postReport(any(ReportReqBody.class), eq(testUser.getId())))
                .thenReturn(reportResBody);

        // when & then
        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportReqBody))
                        .cookie(new Cookie("accessToken", "mock-access-token")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.reportType").value("REVIEW"))
                .andExpect(jsonPath("$.data.targetId").value(400));
    }

    @Test
    @DisplayName("인증되지 않은 사용자의 신고 생성 실패")
    void createReport_Unauthorized() throws Exception {
        // given
        ReportReqBody reportReqBody = new ReportReqBody(
                ReportType.POST,
                100L,
                "Test comment"
        );

        when(cookieHelper.getCookieValue("accessToken", ""))
                .thenReturn("");
        when(authTokenService.payload(""))
                .thenReturn(null);

        // when & then
        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportReqBody)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        verify(reportService, never()).postReport(any(ReportReqBody.class), anyLong());
    }

    @Test
    @DisplayName("존재하지 않는 대상에 대한 신고 실패")
    void createReport_TargetNotFound() throws Exception {
        // given
        ReportReqBody reportReqBody = new ReportReqBody(
                ReportType.POST,
                999L,
                "존재하지 않는 게시글 신고"
        );

        when(reportService.postReport(any(ReportReqBody.class), eq(testUser.getId())))
                .thenThrow(new ServiceException(HttpStatus.NOT_FOUND, "신고 대상을 찾을 수 없습니다."));

        // when & then
        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportReqBody))
                        .cookie(new Cookie("accessToken", "mock-access-token")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.msg").value("신고 대상을 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("중복 신고 실패")
    void createReport_DuplicateReport() throws Exception {
        // given
        ReportReqBody reportReqBody = new ReportReqBody(
                ReportType.POST,
                100L,
                "중복 신고"
        );

        when(reportService.postReport(any(ReportReqBody.class), eq(testUser.getId())))
                .thenThrow(new ServiceException(HttpStatus.CONFLICT, "이미 신고한 대상입니다."));

        // when & then
        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportReqBody))
                        .cookie(new Cookie("accessToken", "mock-access-token")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.msg").value("이미 신고한 대상입니다."));
    }

    @Test
    @DisplayName("본인 신고 실패")
    void createReport_SelfReport() throws Exception {
        // given
        ReportReqBody reportReqBody = new ReportReqBody(
                ReportType.MEMBER,
                1L,  // testUser 본인의 ID
                "본인 신고"
        );

        when(reportService.postReport(any(ReportReqBody.class), eq(testUser.getId())))
                .thenThrow(new ServiceException(HttpStatus.BAD_REQUEST, "본인은 신고할 수 없습니다."));

        // when & then
        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportReqBody))
                        .cookie(new Cookie("accessToken", "mock-access-token")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.msg").value("본인은 신고할 수 없습니다."));
    }

    @Test
    @DisplayName("reportType만 null인 경우")
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
                        .cookie(new Cookie("accessToken", "mock-access-token")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.msg").value(containsString("reportType")));
    }

    @Test
    @DisplayName("targetId만 null인 경우")
    void createReport_NullTargetId() throws Exception {
        // given
        ReportReqBody invalidReqBody = new ReportReqBody(
                ReportType.POST,
                null,  // targetId null
                "Valid comment"
        );

        // when & then
        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReqBody))
                        .cookie(new Cookie("accessToken", "mock-access-token")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.msg").value(containsString("targetId")));
    }

    @Test
    @DisplayName("comment가 빈 문자열인 경우")
    void createReport_BlankComment() throws Exception {
        // given
        ReportReqBody invalidReqBody = new ReportReqBody(
                ReportType.POST,
                100L,
                ""  // blank comment
        );

        // when & then
        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReqBody))
                        .cookie(new Cookie("accessToken", "mock-access-token")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.msg").value(containsString("comment")));
    }

    @Test
    @DisplayName("comment가 공백만 있는 경우")
    void createReport_WhitespaceComment() throws Exception {
        // given
        ReportReqBody invalidReqBody = new ReportReqBody(
                ReportType.POST,
                100L,
                "   "  // whitespace only
        );

        // when & then
        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReqBody))
                        .cookie(new Cookie("accessToken", "mock-access-token")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.msg").value(containsString("comment")));
    }

    @Test
    @DisplayName("매우 긴 comment로 신고 생성")
    void createReport_LongComment() throws Exception {
        // given
        String longComment = "a".repeat(500);  // 500자 comment
        ReportReqBody reportReqBody = new ReportReqBody(
                ReportType.POST,
                100L,
                longComment
        );

        ReportResBody reportResBody = new ReportResBody(
                5L,
                ReportType.POST,
                100L,
                longComment,
                testUser.getId(),
                LocalDateTime.now()
        );

        when(reportService.postReport(any(ReportReqBody.class), eq(testUser.getId())))
                .thenReturn(reportResBody);

        // when & then
        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportReqBody))
                        .cookie(new Cookie("accessToken", "mock-access-token")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.comment").value(longComment));
    }

    @Test
    @DisplayName("특수문자가 포함된 comment로 신고 생성")
    void createReport_SpecialCharactersComment() throws Exception {
        // given
        String specialComment = "특수문자 테스트 !@#$%^&*()_+-=[]{}|;':\",./<>?";
        ReportReqBody reportReqBody = new ReportReqBody(
                ReportType.POST,
                100L,
                specialComment
        );

        ReportResBody reportResBody = new ReportResBody(
                6L,
                ReportType.POST,
                100L,
                specialComment,
                testUser.getId(),
                LocalDateTime.now()
        );

        when(reportService.postReport(any(ReportReqBody.class), eq(testUser.getId())))
                .thenReturn(reportResBody);

        // when & then
        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportReqBody))
                        .cookie(new Cookie("accessToken", "mock-access-token")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.comment").value(specialComment));
    }
}