package com.back.domain.member.member.controller;

import com.back.domain.member.member.common.MemberRole;
import com.back.domain.member.member.dto.MemberJoinReqBody;
import com.back.domain.member.member.dto.MemberLoginReqBody;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.service.AuthTokenService;
import com.back.domain.member.member.service.MemberService;
import com.back.domain.member.member.service.RefreshTokenStore;
import com.back.global.security.SecurityUser;
import com.back.global.web.CookieHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MemberControllerTest {
    
    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private AuthTokenService authTokenService;

    @MockitoBean
    private RefreshTokenStore refreshTokenStore;

    @MockitoBean
    private CookieHelper cookieHelper;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Member testMember;

    @BeforeEach
    void setup() {
        Member member = Member.builder()
                .email("test@example.com")
                .password("password123")
                .name("테스트")
                .nickname("테스트닉네임")
                .phoneNumber("01012345678")
                .address1("서울시 강남구")
                .address2("테헤란로 123")
                .role(MemberRole.USER)
                .isBanned(false)
                .build();

        // ReflectionTestUtils를 사용하여 ID 설정
        ReflectionTestUtils.setField(member, "id", 1L);
        testMember = member;

        when(memberService.join(any(MemberJoinReqBody.class))).thenReturn(testMember);
        
        // Mock CookieHelper의 기본 동작 설정
        when(cookieHelper.getCookieValue(anyString(), anyString())).thenReturn("");
    }

    @Test
    void joinSuccessTest() throws Exception {
        // given
        MemberJoinReqBody joinRequest = new MemberJoinReqBody(
                "test@example.com",
                "password123",
                "테스트",
                "서울시 강남구",
                "테헤란로 123",
                "테스트닉네임",
                "01012345678"
        );

        when(memberService.join(any())).thenReturn(testMember);

        // when & then
        mockMvc.perform(post("/api/v1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().string("회원가입 되었습니다."));

        verify(memberService).join(any());
    }

    @Test
    void loginSuccessTest() throws Exception {
        // given
        MemberLoginReqBody loginRequest = new MemberLoginReqBody(
                "test@example.com",
                "password123"
        );

        when(memberService.findByEmail(any())).thenReturn(Optional.of(testMember));
        when(authTokenService.genAccessToken(any())).thenReturn("access-token");
        when(authTokenService.issueRefresh(any())).thenReturn("refresh-token");

        // when & then
        mockMvc.perform(post("/api/v1/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.name").value("테스트"))
                .andExpect(jsonPath("$.nickname").value("테스트닉네임"));

        // 쿠키 설정만 검증
        verify(cookieHelper).setCookie("accessToken", "access-token");
        verify(cookieHelper).setCookie("refreshToken", "refresh-token");
    }

    @Test
    void loginFailWithNonExistentEmailTest() throws Exception {
        // given
        MemberLoginReqBody loginRequest = new MemberLoginReqBody(
                "notexist@example.com",
                "password123"
        );

        when(memberService.findByEmail(any())).thenReturn(Optional.empty());

        // when & then
        mockMvc.perform(post("/api/v1/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void logoutSuccessTest() throws Exception {
        // given
        when(cookieHelper.getCookieValue("refreshToken", null)).thenReturn("refresh-token");

        // when & then
        mockMvc.perform(post("/api/v1/members/logout"))
                .andExpect(status().isOk())
                .andExpect(content().string("로그아웃 되었습니다."));

        verify(refreshTokenStore).revoke("refresh-token");
        verify(cookieHelper).deleteCookie("accessToken");
        verify(cookieHelper).deleteCookie("refreshToken");
    }

    @Test
    void getMeSuccessTest() throws Exception {
        // given
        SecurityUser securityUser = new SecurityUser(1L, "test@example.com", "", "테스트닉네임", testMember.getAuthorities());
        UsernamePasswordAuthenticationToken authentication = 
            new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);  // 시큐리티 컨텍스트에 직접 설정
        
        when(memberService.getById(1L)).thenReturn(testMember);

        // when & then
        mockMvc.perform(get("/api/v1/members/me")
                    .with(request -> {  // 요청에 인증 정보 추가
                        request.setUserPrincipal(authentication);
                        return request;
                    }))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.name").value("테스트"))
            .andExpect(jsonPath("$.nickname").value("테스트닉네임"));
    }
}