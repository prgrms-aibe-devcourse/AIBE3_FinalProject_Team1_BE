package com.back.domain.report.controller;

import com.back.domain.member.service.AuthTokenService;
import com.back.domain.member.service.RefreshTokenStore;
import com.back.domain.report.common.ReportType;
import com.back.domain.report.dto.ReportResBody;
import com.back.domain.report.service.ReportService;
import com.back.global.security.SecurityUser;
import com.back.global.web.CookieHelper;
import com.back.standard.util.page.PageMeta;
import com.back.standard.util.page.PagePayload;
import com.back.standard.util.page.SortOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReportAdminControllerTest {

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

    private SecurityUser adminUser;
    private SecurityUser normalUser;

    @BeforeEach
    void setup() {
        // Admin 사용자
        adminUser = new SecurityUser(
                1L,
                "admin@test.com",
                "password",
                "adminUser",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        // 일반 사용자
        normalUser = new SecurityUser(
                2L,
                "user@test.com",
                "password",
                "normalUser",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        // Admin 인증 설정
        when(cookieHelper.getCookieValue("accessToken", ""))
                .thenReturn("mock-admin-token");
        when(cookieHelper.getCookieValue("refreshToken", null))
                .thenReturn("mock-refresh-token");

        Map<String, Object> adminClaims = new HashMap<>();
        adminClaims.put("id", adminUser.getId());
        adminClaims.put("email", adminUser.getUsername());
        adminClaims.put("nickname", adminUser.getNickname());
        adminClaims.put("role", "ADMIN");
        adminClaims.put("authVersion", 1L);

        when(authTokenService.payload("mock-admin-token"))
                .thenReturn(adminClaims);

        when(refreshTokenStore.getAuthVersion(adminUser.getId()))
                .thenReturn(1L);
    }

    @Test
    @DisplayName("관리자 - 모든 신고 목록 조회 성공")
    void getReports_AllTypes_Success() throws Exception {
        // given
        List<ReportResBody> reports = List.of(
                new ReportResBody(1L, ReportType.POST, 100L, "신고 내용 1", 10L, LocalDateTime.now()),
                new ReportResBody(2L, ReportType.MEMBER, 200L, "신고 내용 2", 11L, LocalDateTime.now()),
                new ReportResBody(3L, ReportType.REVIEW, 300L, "신고 내용 3", 12L, LocalDateTime.now())
        );

        PageMeta pageMeta = new PageMeta(
                0, 10, 3, 1,
                true, true, false, false,
                List.of(new SortOrder("id", "DESC"))
        );

        PagePayload<ReportResBody> pagePayload = new PagePayload<>(reports, pageMeta);

        when(reportService.getReports(any(), eq(null)))
                .thenReturn(pagePayload);

        // when & then
        mockMvc.perform(get("/api/v1/adm/reports")
                        .cookie(new Cookie("accessToken", "mock-admin-token")))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("OK"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(3))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].reportType").value("POST"))
                .andExpect(jsonPath("$.data.content[1].reportType").value("MEMBER"))
                .andExpect(jsonPath("$.data.content[2].reportType").value("REVIEW"))
                .andExpect(jsonPath("$.data.page.page").value(0))
                .andExpect(jsonPath("$.data.page.size").value(10))
                .andExpect(jsonPath("$.data.page.totalElements").value(3))
                .andExpect(jsonPath("$.data.page.totalPages").value(1));

        verify(reportService, times(1)).getReports(any(), eq(null));
    }

    @Test
    @DisplayName("관리자 - POST 타입만 필터링하여 조회")
    void getReports_FilterByPostType_Success() throws Exception {
        // given
        List<ReportResBody> reports = List.of(
                new ReportResBody(1L, ReportType.POST, 100L, "게시글 신고 1", 10L, LocalDateTime.now()),
                new ReportResBody(2L, ReportType.POST, 101L, "게시글 신고 2", 11L, LocalDateTime.now())
        );

        PageMeta pageMeta = new PageMeta(
                0, 10, 2, 1,
                true, true, false, false,
                List.of(new SortOrder("id", "DESC"))
        );

        PagePayload<ReportResBody> pagePayload = new PagePayload<>(reports, pageMeta);

        when(reportService.getReports(any(), eq(ReportType.POST)))
                .thenReturn(pagePayload);

        // when & then
        mockMvc.perform(get("/api/v1/adm/reports")
                        .param("reportType", "POST")
                        .cookie(new Cookie("accessToken", "mock-admin-token")))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].reportType").value("POST"))
                .andExpect(jsonPath("$.data.content[1].reportType").value("POST"))
                .andExpect(jsonPath("$.data.page.totalElements").value(2));

        verify(reportService, times(1)).getReports(any(), eq(ReportType.POST));
    }

    @Test
    @DisplayName("관리자 - MEMBER 타입만 필터링하여 조회")
    void getReports_FilterByMemberType_Success() throws Exception {
        // given
        List<ReportResBody> reports = List.of(
                new ReportResBody(3L, ReportType.MEMBER, 200L, "회원 신고", 12L, LocalDateTime.now())
        );

        PageMeta pageMeta = new PageMeta(
                0, 10, 1, 1,
                true, true, false, false,
                List.of(new SortOrder("id", "DESC"))
        );

        PagePayload<ReportResBody> pagePayload = new PagePayload<>(reports, pageMeta);

        when(reportService.getReports(any(), eq(ReportType.MEMBER)))
                .thenReturn(pagePayload);

        // when & then
        mockMvc.perform(get("/api/v1/adm/reports")
                        .param("reportType", "MEMBER")
                        .cookie(new Cookie("accessToken", "mock-admin-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].reportType").value("MEMBER"));

        verify(reportService, times(1)).getReports(any(), eq(ReportType.MEMBER));
    }

    @Test
    @DisplayName("관리자 - REVIEW 타입만 필터링하여 조회")
    void getReports_FilterByReviewType_Success() throws Exception {
        // given
        List<ReportResBody> reports = List.of(
                new ReportResBody(4L, ReportType.REVIEW, 300L, "리뷰 신고", 13L, LocalDateTime.now())
        );

        PageMeta pageMeta = new PageMeta(
                0, 10, 1, 1,
                true, true, false, false,
                List.of(new SortOrder("id", "DESC"))
        );

        PagePayload<ReportResBody> pagePayload = new PagePayload<>(reports, pageMeta);

        when(reportService.getReports(any(), eq(ReportType.REVIEW)))
                .thenReturn(pagePayload);

        // when & then
        mockMvc.perform(get("/api/v1/adm/reports")
                        .param("reportType", "REVIEW")
                        .cookie(new Cookie("accessToken", "mock-admin-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].reportType").value("REVIEW"));

        verify(reportService, times(1)).getReports(any(), eq(ReportType.REVIEW));
    }

    @Test
    @DisplayName("관리자 - 페이징 테스트 (첫 페이지)")
    void getReports_Pagination_FirstPage() throws Exception {
        // given
        List<ReportResBody> reports = List.of(
                new ReportResBody(1L, ReportType.POST, 100L, "신고 1", 10L, LocalDateTime.now()),
                new ReportResBody(2L, ReportType.POST, 101L, "신고 2", 11L, LocalDateTime.now())
        );

        PageMeta pageMeta = new PageMeta(
                0, 2, 5, 3,  // 전체 5개, 3페이지
                true, false, true, false,
                List.of(new SortOrder("id", "DESC"))
        );

        PagePayload<ReportResBody> pagePayload = new PagePayload<>(reports, pageMeta);

        when(reportService.getReports(any(), eq(null)))
                .thenReturn(pagePayload);

        // when & then
        mockMvc.perform(get("/api/v1/adm/reports")
                        .param("page", "0")
                        .param("size", "2")
                        .cookie(new Cookie("accessToken", "mock-admin-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.page.page").value(0))
                .andExpect(jsonPath("$.data.page.size").value(2))
                .andExpect(jsonPath("$.data.page.totalElements").value(5))
                .andExpect(jsonPath("$.data.page.totalPages").value(3))
                .andExpect(jsonPath("$.data.page.first").value(true))
                .andExpect(jsonPath("$.data.page.last").value(false))
                .andExpect(jsonPath("$.data.page.hasNext").value(true))
                .andExpect(jsonPath("$.data.page.hasPrevious").value(false));
    }

    @Test
    @DisplayName("관리자 - 페이징 테스트 (두 번째 페이지)")
    void getReports_Pagination_SecondPage() throws Exception {
        // given
        List<ReportResBody> reports = List.of(
                new ReportResBody(3L, ReportType.MEMBER, 102L, "신고 3", 12L, LocalDateTime.now()),
                new ReportResBody(4L, ReportType.REVIEW, 103L, "신고 4", 13L, LocalDateTime.now())
        );

        PageMeta pageMeta = new PageMeta(
                1, 2, 5, 3,
                false, false, true, true,
                List.of(new SortOrder("id", "DESC"))
        );

        PagePayload<ReportResBody> pagePayload = new PagePayload<>(reports, pageMeta);

        when(reportService.getReports(any(), eq(null)))
                .thenReturn(pagePayload);

        // when & then
        mockMvc.perform(get("/api/v1/adm/reports")
                        .param("page", "1")
                        .param("size", "2")
                        .cookie(new Cookie("accessToken", "mock-admin-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page.page").value(1))
                .andExpect(jsonPath("$.data.page.first").value(false))
                .andExpect(jsonPath("$.data.page.last").value(false))
                .andExpect(jsonPath("$.data.page.hasNext").value(true))
                .andExpect(jsonPath("$.data.page.hasPrevious").value(true));
    }

    @Test
    @DisplayName("관리자 - 빈 결과 조회")
    void getReports_EmptyResult() throws Exception {
        // given
        PageMeta pageMeta = new PageMeta(
                0, 10, 0, 0,
                true, true, false, false,
                List.of(new SortOrder("id", "DESC"))
        );

        PagePayload<ReportResBody> pagePayload = new PagePayload<>(List.of(), pageMeta);

        when(reportService.getReports(any(), eq(null)))
                .thenReturn(pagePayload);

        // when & then
        mockMvc.perform(get("/api/v1/adm/reports")
                        .cookie(new Cookie("accessToken", "mock-admin-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.page.totalElements").value(0))
                .andExpect(jsonPath("$.data.page.totalPages").value(0));
    }

    @Test
    @DisplayName("관리자 - 정렬 파라미터 테스트")
    void getReports_WithSortParameter() throws Exception {
        // given
        List<ReportResBody> reports = List.of(
                new ReportResBody(5L, ReportType.POST, 100L, "신고", 10L, LocalDateTime.now())
        );

        PageMeta pageMeta = new PageMeta(
                0, 10, 1, 1,
                true, true, false, false,
                List.of(new SortOrder("createdAt", "ASC"))
        );

        PagePayload<ReportResBody> pagePayload = new PagePayload<>(reports, pageMeta);

        when(reportService.getReports(any(), eq(null)))
                .thenReturn(pagePayload);

        // when & then
        mockMvc.perform(get("/api/v1/adm/reports")
                        .param("sort", "createdAt,asc")
                        .cookie(new Cookie("accessToken", "mock-admin-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page.sort[0].property").value("createdAt"))
                .andExpect(jsonPath("$.data.page.sort[0].direction").value("ASC"));
    }

    @Test
    @DisplayName("일반 사용자 - 접근 거부")
    void getReports_Forbidden_NormalUser() throws Exception {
        // given - 일반 사용자로 Mock 재설정
        when(cookieHelper.getCookieValue("accessToken", ""))
                .thenReturn("mock-user-token");  // ✅ 다른 토큰
        when(cookieHelper.getCookieValue("refreshToken", null))
                .thenReturn("mock-refresh-token");

        Map<String, Object> userClaims = new HashMap<>();
        userClaims.put("id", normalUser.getId());
        userClaims.put("email", normalUser.getUsername());
        userClaims.put("nickname", normalUser.getNickname());
        userClaims.put("role", "USER");  // ✅ USER 권한
        userClaims.put("authVersion", 1L);

        when(authTokenService.payload("mock-user-token"))  // ✅ 새 토큰에 대한 설정
                .thenReturn(userClaims);

        when(refreshTokenStore.getAuthVersion(normalUser.getId()))
                .thenReturn(1L);

        // when & then
        mockMvc.perform(get("/api/v1/adm/reports")
                        .cookie(new Cookie("accessToken", "mock-user-token")))  // ✅ 새 토큰 사용
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.msg").value("권한이 없습니다."));

        verifyNoInteractions(reportService);
    }

    @Test
    @DisplayName("인증되지 않은 사용자 - 접근 거부")
    void getReports_Unauthorized() throws Exception {
        // given - 인증 정보 없음
        when(cookieHelper.getCookieValue("accessToken", ""))
                .thenReturn("");
        when(cookieHelper.getCookieValue("refreshToken", null))
                .thenReturn(null);
        when(authTokenService.payload(""))
                .thenReturn(null);

        // when & then
        mockMvc.perform(get("/api/v1/adm/reports"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.msg").value("로그인 후 이용해주세요."));

        verifyNoInteractions(reportService);
    }

    @Test
    @DisplayName("관리자 - 잘못된 reportType 파라미터")
    void getReports_InvalidReportType() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/adm/reports")
                        .param("reportType", "INVALID_TYPE")
                        .cookie(new Cookie("accessToken", "mock-admin-token")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(reportService);
    }

    @Test
    @DisplayName("관리자 - 여러 필터와 페이징 조합")
    void getReports_CombinedFiltersAndPaging() throws Exception {
        // given
        List<ReportResBody> reports = List.of(
                new ReportResBody(1L, ReportType.POST, 100L, "신고 1", 10L, LocalDateTime.now())
        );

        PageMeta pageMeta = new PageMeta(
                2, 5, 15, 3,
                false, true, false, true,
                List.of(new SortOrder("id", "DESC"))
        );

        PagePayload<ReportResBody> pagePayload = new PagePayload<>(reports, pageMeta);

        when(reportService.getReports(any(), eq(ReportType.POST)))
                .thenReturn(pagePayload);

        // when & then
        mockMvc.perform(get("/api/v1/adm/reports")
                        .param("reportType", "POST")
                        .param("page", "2")
                        .param("size", "5")
                        .param("sort", "id,desc")
                        .cookie(new Cookie("accessToken", "mock-admin-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.page.page").value(2))
                .andExpect(jsonPath("$.data.page.size").value(5))
                .andExpect(jsonPath("$.data.page.last").value(true));

        verify(reportService, times(1)).getReports(any(), eq(ReportType.POST));
    }
}