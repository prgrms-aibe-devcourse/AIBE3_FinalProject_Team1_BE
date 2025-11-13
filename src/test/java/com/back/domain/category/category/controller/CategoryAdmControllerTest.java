
package com.back.domain.category.category.controller;

import com.back.domain.category.dto.CategoryCreateReqBody;
import com.back.domain.category.dto.CategoryResBody;
import com.back.domain.category.dto.CategoryUpdateReqBody;
import com.back.domain.category.service.CategoryService;
import com.back.domain.member.service.AuthTokenService;
import com.back.domain.member.service.RefreshTokenStore;
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
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CategoryAdmControllerTest {

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private CookieHelper cookieHelper;

    @MockitoBean
    private AuthTokenService authTokenService;

    @MockitoBean
    private RefreshTokenStore refreshTokenStore;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private SecurityUser adminUser;

    @BeforeEach
    void setup() {
        // 관리자 계정 설정
        adminUser = new SecurityUser(
                1L,
                "admin@example.com",
                "password",
                "관리자",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        // Mock accessToken 설정
        when(cookieHelper.getCookieValue("accessToken", ""))
                .thenReturn("mock-access-token");

        // accessToken 검증 모의 설정
        Map<String, Object> mockClaims = new HashMap<>();
        mockClaims.put("id", adminUser.getId());
        mockClaims.put("email", adminUser.getUsername());
        mockClaims.put("nickname", adminUser.getNickname());
        mockClaims.put("role", "ADMIN");
        mockClaims.put("authVersion", 1L);

        when(authTokenService.payload("mock-access-token"))
                .thenReturn(mockClaims);

        // authVersion 검증 모의 설정
        when(refreshTokenStore.getAuthVersion(adminUser.getId()))
                .thenReturn(1L);
    }

    @Test
    @DisplayName("카테고리 생성 성공")
    void createCategory_success() throws Exception {
        // given
        CategoryCreateReqBody reqBody = new CategoryCreateReqBody(null, "새 카테고리");
        CategoryResBody resBody = new CategoryResBody(1L, "새 카테고리", List.of());
        when(categoryService.createCategory(any(CategoryCreateReqBody.class))).thenReturn(resBody);

        // when & then
        mockMvc.perform(post("/api/v1/adm/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBody))
                        .cookie(new Cookie("accessToken", "mock-access-token")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.msg").value("카테고리 등록 성공"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("새 카테고리"))
                .andExpect(jsonPath("$.data.child").isEmpty());

        verify(categoryService).createCategory(any(CategoryCreateReqBody.class));
    }

    @Test
    @DisplayName("카테고리 수정 성공")
    void updateCategory_success() throws Exception {
        // given
        CategoryUpdateReqBody reqBody = new CategoryUpdateReqBody("수정된 카테고리");
        CategoryResBody resBody = new CategoryResBody(1L, "수정된 카테고리", List.of());
        when(categoryService.updateCategory(eq(1L), any(CategoryUpdateReqBody.class))).thenReturn(resBody);

        // when & then
        mockMvc.perform(patch("/api/v1/adm/categories/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBody))
                        .cookie(new Cookie("accessToken", "mock-access-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("카테고리 수정 성공"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("수정된 카테고리"))
                .andExpect(jsonPath("$.data.child").isEmpty());

        verify(categoryService).updateCategory(eq(1L), any(CategoryUpdateReqBody.class));
    }

    @Test
    @DisplayName("카테고리 삭제 성공")
    void deleteCategory_success() throws Exception {
        // given
        doNothing().when(categoryService).deleteCategory(1L);

        // when & then
        mockMvc.perform(delete("/api/v1/adm/categories/{id}", 1L)
                        .cookie(new Cookie("accessToken", "mock-access-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("카테고리 삭제 성공"))
                .andExpect(jsonPath("$.data").doesNotExist()); // data 없음

        verify(categoryService).deleteCategory(1L);
    }

    @Test
    @DisplayName("인증되지 않은 사용자의 카테고리 생성 시도")
    void createCategory_unauthorized() throws Exception {
        when(cookieHelper.getCookieValue("accessToken", ""))
                .thenReturn("");
        when(cookieHelper.getCookieValue("refreshToken", ""))
                .thenReturn("");

        // 토큰 검증 실패 시뮬레이션
        when(authTokenService.payload(anyString()))
                .thenReturn(null);
        // given
        CategoryCreateReqBody reqBody = new CategoryCreateReqBody(null, "새 카테고리");

        // when & then
        mockMvc.perform(post("/api/v1/adm/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBody)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(categoryService);
    }

    @Test
    @DisplayName("잘못된 입력으로 카테고리 생성 실패")
    void createCategory_invalidInput() throws Exception {
        // given
        CategoryCreateReqBody reqBody = new CategoryCreateReqBody(null, "   ");

        // when & then
        mockMvc.perform(post("/api/v1/adm/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBody))
                        .cookie(new Cookie("accessToken", "mock-access-token")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(categoryService);
    }
}