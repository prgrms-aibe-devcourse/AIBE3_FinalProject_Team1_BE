package com.back.domain.member.controller;

import com.back.config.TestConfig;
import com.back.domain.member.dto.*;
import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import com.back.domain.member.service.AuthTokenService;
import com.back.domain.member.service.EmailService;
import com.back.domain.member.service.MemberService;
import com.back.domain.member.service.RefreshTokenStore;
import com.back.global.s3.S3Uploader;
import com.back.global.security.SecurityUser;
import com.back.global.web.CookieHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(TestConfig.class)
@AutoConfigureMockMvc
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MemberControllerTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private AuthTokenService authTokenService;

    @Autowired
    private RefreshTokenStore refreshTokenStore;

    @MockitoBean
    private CookieHelper cookieHelper;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private S3Uploader s3Uploader;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        when(s3Uploader.uploadProfileOriginal(any(MultipartFile.class)))
                .thenReturn("https://bucket.s3.ap-northeast-2.amazonaws.com/post/test.jpg");
        when(s3Uploader.generatePresignedUrl(anyString()))
                .thenReturn("https://s3.example.com/test.jpg");
        when(s3Uploader.generatePresignedUrl(null))
                .thenReturn(null);

        when(cookieHelper.getCookieValue(anyString(), anyString()))
                .thenReturn("");
        doNothing().when(cookieHelper).setCookie(anyString(), anyString());
        doNothing().when(cookieHelper).deleteCookie(anyString());
    }

    @Test
    @Order(1)
    @DisplayName("회원가입 - 성공")
    void join_Success() throws Exception {
        // given
        MemberJoinReqBody joinRequest = new MemberJoinReqBody(
                "test@example.com",
                "password123",
                "테스트닉네임"
        );

        // when & then
        mockMvc.perform(post("/api/v1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.msg").value("회원가입 되었습니다."))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("테스트닉네임"));

        Member saved = memberRepository.findByEmail("test@example.com").orElse(null);
        assertThat(saved).isNotNull();
        assertThat(saved.getNickname()).isEqualTo("테스트닉네임");
    }

    @Test
    @Order(2)
    @DisplayName("회원가입 - 실패: 이메일 중복")
    void join_Fail_DuplicateEmail() throws Exception {
        // given
        MemberJoinReqBody firstRequest = new MemberJoinReqBody(
                "duplicate@example.com",
                "password123",
                "테스트닉네임"
        );
        memberService.join(firstRequest);

        // when
        MemberJoinReqBody duplicateRequest = new MemberJoinReqBody(
                "duplicate@example.com",
                "password456",
                "다른닉네임"
        );

        // then
        mockMvc.perform(post("/api/v1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.msg").value("이미 가입된 이메일입니다."));
    }

    @Test
    @Order(3)
    @DisplayName("회원가입 - 실패: 유효성 검증 (이메일 형식 오류)")
    void join_Fail_InvalidEmail() throws Exception {
        // given
        MemberJoinReqBody joinRequest = new MemberJoinReqBody(
                "invalid-email",
                "password123",
                "테스트닉네임"
        );

        // when & then
        mockMvc.perform(post("/api/v1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(4)
    @DisplayName("회원가입 - 실패: 유효성 검증 (필수 필드 누락)")
    void join_Fail_MissingRequiredFields() throws Exception {
        // given
        String invalidRequest = "{\"email\":\"test@example.com\"}";

        // when & then
        mockMvc.perform(post("/api/v1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(5)
    @DisplayName("로그인 - 성공")
    void login_Success() throws Exception {
        // given - 회원 가입
        MemberJoinReqBody joinRequest = new MemberJoinReqBody(
                "login@example.com",
                "password123",
                "로그인테스트"
        );
        memberService.join(joinRequest);

        // when
        MemberLoginReqBody loginRequest = new MemberLoginReqBody(
                "login@example.com",
                "password123"
        );

        // then
        mockMvc.perform(post("/api/v1/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("로그인 되었습니다."))
                .andExpect(jsonPath("$.data.email").value("login@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("로그인테스트"));

        verify(cookieHelper, times(1)).setCookie(eq("accessToken"), anyString());
        verify(cookieHelper, times(1)).setCookie(eq("refreshToken"), anyString());
    }

    @Test
    @Order(6)
    @DisplayName("로그인 - 실패: 존재하지 않는 이메일")
    void login_Fail_NonExistentEmail() throws Exception {
        // given
        MemberLoginReqBody loginRequest = new MemberLoginReqBody(
                "notexist@example.com",
                "password123"
        );

        // when & then
        mockMvc.perform(post("/api/v1/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.msg").value("사용자를 찾을 수 없습니다."));
    }

    @Test
    @Order(7)
    @DisplayName("로그인 - 실패: 비밀번호 불일치")
    void login_Fail_WrongPassword() throws Exception {
        // given
        MemberJoinReqBody joinRequest = new MemberJoinReqBody(
                "pwtest@example.com",
                "password123",
                "비번테스트"
        );
        memberService.join(joinRequest);

        // when
        MemberLoginReqBody loginRequest = new MemberLoginReqBody(
                "pwtest@example.com",
                "wrongpassword"
        );

        // then
        mockMvc.perform(post("/api/v1/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.msg").value("비밀번호가 올바르지 않습니다."));
    }

    @Test
    @Order(8)
    @DisplayName("로그인 - 실패: 유효성 검증")
    void login_Fail_ValidationError() throws Exception {
        // given
        String invalidRequest = "{\"email\":\"invalid-email\"}";

        // when & then
        mockMvc.perform(post("/api/v1/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(9)
    @DisplayName("로그아웃 - 성공")
    void logout_Success() throws Exception {
        // given
        MemberJoinReqBody joinRequest = new MemberJoinReqBody(
                "logout@example.com",
                "password123",
                "로그아웃테스트"
        );
        Member member = memberService.join(joinRequest);

        SecurityUser securityUser = new SecurityUser(
                member.getId(),
                member.getEmail(),
                "",
                member.getNickname(),
                member.getAuthorities()
        );

        String refreshToken = authTokenService.issueRefresh(member);

        when(cookieHelper.getCookieValue(anyString(), anyString()))
                .thenReturn(refreshToken);

        // when & then
        mockMvc.perform(post("/api/v1/members/logout")
                        .with(user(securityUser)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("로그아웃 되었습니다."));

        assertThat(refreshTokenStore.findRefreshPayload(refreshToken)).isNull();

        verify(cookieHelper).deleteCookie("accessToken");
        verify(cookieHelper).deleteCookie("refreshToken");
    }

    @Test
    @Order(10)
    @DisplayName("로그아웃 - 성공: Refresh Token이 없어도 성공")
    void logout_Success_WithoutRefreshToken() throws Exception {
        // given
        MemberJoinReqBody joinRequest = new MemberJoinReqBody(
                "logout2@example.com",
                "password123",
                "로그아웃테스트2"
        );
        Member member = memberService.join(joinRequest);

        SecurityUser securityUser = new SecurityUser(
                member.getId(),
                member.getEmail(),
                "",
                member.getNickname(),
                member.getAuthorities()
        );

        // when & then
        mockMvc.perform(post("/api/v1/members/logout")
                        .with(user(securityUser)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("로그아웃 되었습니다."));
    }

    // ========== 내 정보 조회 테스트 ==========

    @Test
    @Order(11)
    @DisplayName("내 정보 조회 - 성공")
    void getMe_Success() throws Exception {
        // given - 회원 가입
        MemberJoinReqBody joinRequest = new MemberJoinReqBody(
                "me@example.com",
                "password123",
                "내정보"
        );
        Member member = memberService.join(joinRequest);

        SecurityUser securityUser = new SecurityUser(
                member.getId(),
                member.getEmail(),
                "",
                member.getNickname(),
                member.getAuthorities()
        );

        // when & then
        mockMvc.perform(get("/api/v1/members/me")
                        .with(user(securityUser)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("현재 회원 정보입니다."))
                .andExpect(jsonPath("$.data.email").value("me@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("내정보"));
    }

    @Test
    @Order(12)
    @DisplayName("내 정보 조회 - 실패: 인증 없음")
    void getMe_Fail_Unauthorized() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/members/me"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(13)
    @DisplayName("내 정보 조회 - 실패: 존재하지 않는 회원")
    void getMe_Fail_MemberNotFound() throws Exception {
        // given
        SecurityUser securityUser = new SecurityUser(
                999L,
                "notexist@example.com",
                "",
                "존재안함",
                List.of()
        );

        // when & then
        mockMvc.perform(get("/api/v1/members/me")
                        .with(user(securityUser)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 회원입니다."));
    }

    @Test
    @Order(14)
    @DisplayName("내 정보 수정 - 성공: 기본 정보만")
    void updateMe_Success_BasicInfo() throws Exception {
        // given - 회원 가입
        MemberJoinReqBody joinRequest = new MemberJoinReqBody(
                "update@example.com",
                "password123",
                "수정전"
        );
        Member member = memberService.join(joinRequest);

        SecurityUser securityUser = new SecurityUser(
                member.getId(),
                member.getEmail(),
                "",
                member.getNickname(),
                member.getAuthorities()
        );

        MemberUpdateReqBody updateRequest = new MemberUpdateReqBody(
                "서울시 강남구",
                "테헤란로 123",
                "홍길동",
                "010-1234-5678",
                false
        );

        // when & then
        mockMvc.perform(multipart("/api/v1/members/me")
                        .file(new MockMultipartFile(
                                "reqBody",
                                "",
                                MediaType.APPLICATION_JSON_VALUE,
                                objectMapper.writeValueAsBytes(updateRequest)
                        ))
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        })
                        .with(user(securityUser)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("회원 정보가 수정되었습니다."))
                .andExpect(jsonPath("$.data.name").value("홍길동"))
                .andExpect(jsonPath("$.data.phoneNumber").value("010-1234-5678"));

        Member updated = memberRepository.findById(member.getId()).orElse(null);
        assertThat(updated).isNotNull();
        assertThat(updated.getName()).isEqualTo("홍길동");
        assertThat(updated.getPhoneNumber()).isEqualTo("010-1234-5678");
        assertThat(updated.getAddress1()).isEqualTo("서울시 강남구");
        assertThat(updated.getAddress2()).isEqualTo("테헤란로 123");
    }

    @Test
    @Order(15)
    @DisplayName("내 정보 수정 - 성공: 프로필 이미지 포함")
    void updateMe_Success_WithProfileImage() throws Exception {
        // given
        MemberJoinReqBody joinRequest = new MemberJoinReqBody(
                "update2@example.com",
                "password123",
                "수정전2"
        );
        Member member = memberService.join(joinRequest);

        SecurityUser securityUser = new SecurityUser(
                member.getId(),
                member.getEmail(),
                "",
                member.getNickname(),
                member.getAuthorities()
        );

        MemberUpdateReqBody updateRequest = new MemberUpdateReqBody(
                "서울시 서초구",
                "강남대로 456",
                "김철수",
                "010-9876-5432",
                false
        );

        MockMultipartFile profileImage = new MockMultipartFile(
                "profileImage",
                "test.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "test image content".getBytes()
        );

        when(s3Uploader.uploadProfileOriginal(any(MultipartFile.class)))
                .thenReturn("https://bucket.s3.ap-northeast-2.amazonaws.com/post/test.jpg");

        // when & then
        mockMvc.perform(multipart("/api/v1/members/me")
                        .file(profileImage)
                        .file(new MockMultipartFile(
                                "reqBody",
                                "",
                                MediaType.APPLICATION_JSON_VALUE,
                                objectMapper.writeValueAsBytes(updateRequest)
                        ))
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        })
                        .with(user(securityUser)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("회원 정보가 수정되었습니다."));

        verify(s3Uploader).uploadProfileOriginal(any(MultipartFile.class));
    }

    @Test
    @Order(16)
    @DisplayName("내 정보 수정 - 실패: 인증 없음")
    void updateMe_Fail_Unauthorized() throws Exception {
        // given
        MemberUpdateReqBody updateRequest = new MemberUpdateReqBody(
                "서울시 종로구",
                "세종대로 1",
                "이영희",
                "010-1111-2222",
                false
        );

        // when & then
        mockMvc.perform(multipart("/api/v1/members/me")
                        .file(new MockMultipartFile(
                                "reqBody",
                                "",
                                MediaType.APPLICATION_JSON_VALUE,
                                objectMapper.writeValueAsBytes(updateRequest)
                        ))
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        }))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(17)
    @DisplayName("타인 정보 조회 - 성공")
    void getMember_Success() throws Exception {
        // given - 회원 가입
        MemberJoinReqBody joinRequest = new MemberJoinReqBody(
                "other@example.com",
                "password123",
                "타인"
        );
        Member member = memberService.join(joinRequest);

        // when & then
        mockMvc.perform(get("/api/v1/members/{id}", member.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("회원 정보입니다."))
                .andExpect(jsonPath("$.data.id").value(member.getId()))
                .andExpect(jsonPath("$.data.nickname").value("타인"));
    }

    @Test
    @Order(18)
    @DisplayName("타인 정보 조회 - 실패: 존재하지 않는 회원")
    void getMember_Fail_NotFound() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/members/{id}", 999L))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 회원입니다."));
    }

    @Test
    @Order(19)
    @DisplayName("닉네임 중복 확인 - 중복 아님")
    void checkNickname_NotDuplicated() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/members/check-nickname")
                        .param("nickname", "새로운닉네임"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("닉네임 중복 확인 완료"))
                .andExpect(jsonPath("$.data.isDuplicated").value(false));
    }

    @Test
    @Order(20)
    @DisplayName("닉네임 중복 확인 - 중복됨")
    void checkNickname_Duplicated() throws Exception {
        // given - 회원 가입
        MemberJoinReqBody joinRequest = new MemberJoinReqBody(
                "nickname@example.com",
                "password123",
                "중복닉네임"
        );
        memberService.join(joinRequest);

        // when & then
        mockMvc.perform(get("/api/v1/members/check-nickname")
                        .param("nickname", "중복닉네임"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("닉네임 중복 확인 완료"))
                .andExpect(jsonPath("$.data.isDuplicated").value(true));
    }

    @Test
    @Order(21)
    @DisplayName("이메일 인증 코드 발송 - 성공")
    void sendVerificationCode_Success() throws Exception {
        // given
        MemberSendCodeReqBody request = new MemberSendCodeReqBody("newuser@example.com");
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5);

        when(emailService.sendVerificationCode("newuser@example.com"))
                .thenReturn(expiresAt);

        // when & then
        mockMvc.perform(post("/api/v1/members/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("이메일 인증이 발송되었습니다."))
                .andExpect(jsonPath("$.data.expiresIn").exists());

        verify(emailService).sendVerificationCode("newuser@example.com");
    }

    @Test
    @Order(22)
    @DisplayName("이메일 인증 코드 발송 - 실패: 이미 가입된 이메일")
    void sendVerificationCode_Fail_AlreadyExists() throws Exception {
        // given - 회원 가입
        MemberJoinReqBody joinRequest = new MemberJoinReqBody(
                "existing@example.com",
                "password123",
                "기존회원"
        );
        memberService.join(joinRequest);

        MemberSendCodeReqBody request = new MemberSendCodeReqBody("existing@example.com");

        // when & then
        mockMvc.perform(post("/api/v1/members/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.msg").value("이미 가입된 이메일입니다."));
    }

    @Test
    @Order(23)
    @DisplayName("이메일 인증 코드 발송 - 실패: 유효성 검증")
    void sendVerificationCode_Fail_InvalidEmail() throws Exception {
        // given
        String invalidRequest = "{\"email\":\"invalid-email\"}";

        // when & then
        mockMvc.perform(post("/api/v1/members/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(24)
    @DisplayName("이메일 인증 코드 검증 - 성공")
    void verifyCode_Success() throws Exception {
        // given
        MemberVerifyReqBody request = new MemberVerifyReqBody("test@example.com", "123456");

        doNothing().when(emailService).verifyCode("test@example.com", "123456");

        // when & then
        mockMvc.perform(post("/api/v1/members/verify-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("이메일 인증이 완료되었습니다."))
                .andExpect(jsonPath("$.data.isVerified").value(true));

        verify(emailService).verifyCode("test@example.com", "123456");
    }

    @Test
    @Order(25)
    @DisplayName("이메일 인증 코드 검증 - 실패: 코드 불일치")
    void verifyCode_Fail_WrongCode() throws Exception {
        // given
        MemberVerifyReqBody request = new MemberVerifyReqBody("test@example.com", "999999");

        doThrow(new RuntimeException("인증 코드가 일치하지 않습니다."))
                .when(emailService).verifyCode("test@example.com", "999999");

        // when & then
        mockMvc.perform(post("/api/v1/members/verify-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isInternalServerError());
    }

    @Test
    @Order(26)
    @DisplayName("이메일 인증 코드 검증 - 실패: 만료된 코드")
    void verifyCode_Fail_ExpiredCode() throws Exception {
        // given
        MemberVerifyReqBody request = new MemberVerifyReqBody("test@example.com", "123456");

        doThrow(new RuntimeException("인증 코드가 만료되었습니다."))
                .when(emailService).verifyCode("test@example.com", "123456");

        // when & then
        mockMvc.perform(post("/api/v1/members/verify-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isInternalServerError());
    }
}