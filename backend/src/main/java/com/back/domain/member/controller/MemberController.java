package com.back.domain.member.controller;

import com.back.domain.member.dto.*;
import com.back.domain.member.entity.Member;
import com.back.domain.member.service.AuthTokenService;
import com.back.domain.member.service.EmailService;
import com.back.domain.member.service.MemberService;
import com.back.domain.member.service.RefreshTokenStore;
import com.back.domain.review.service.ReviewSummaryService;
import com.back.global.exception.ServiceException;
import com.back.global.rsData.RsData;
import com.back.global.s3.S3Uploader;
import com.back.global.security.SecurityUser;
import com.back.global.web.CookieHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController implements MemberApi{
    private final MemberService memberService;
    private final AuthTokenService authTokenService;
    private final RefreshTokenStore refreshTokenStore;
    private final CookieHelper cookieHelper;
    private final EmailService emailService;
    private final S3Uploader s3Uploader;
    private final ReviewSummaryService reviewSummaryService;

    @PostMapping
    public ResponseEntity<RsData<MemberDto>> join(
            @Valid @RequestBody MemberJoinReqBody reqBody
    ) {
        Member member =memberService.join(reqBody);
        String presignedUrl = s3Uploader.generatePresignedUrl(member.getProfileImgUrl());
        return ResponseEntity.status(201).body(new RsData<>(HttpStatus.CREATED, "회원가입 되었습니다.", new MemberDto(member, presignedUrl)));
    }

    @PostMapping("/login")
    public ResponseEntity<RsData<MemberDto>> login(
            @Valid @RequestBody MemberLoginReqBody reqBody
    ) {
        Member member = memberService.findByEmail(reqBody.email()).orElseThrow(
                () -> new ServiceException(HttpStatus.UNAUTHORIZED, "사용자를 찾을 수 없습니다.")
        );
        memberService.checkPassword(member, reqBody.password());

        String accessToken = authTokenService.genAccessToken(member);
        String refreshToken = authTokenService.issueRefresh(member);

        cookieHelper.setCookie("accessToken", accessToken);
        cookieHelper.setCookie("refreshToken", refreshToken);

        String presignedUrl = s3Uploader.generatePresignedUrl(member.getProfileImgUrl());
        return ResponseEntity.ok(new RsData<>(HttpStatus.OK, "로그인 되었습니다.", new MemberDto(member, presignedUrl)));
    }

    @PostMapping("/logout")
    public ResponseEntity<RsData<Void>> logout() {
        String refreshPlain = cookieHelper.getCookieValue("refreshToken", null);
        if (refreshPlain != null && !refreshPlain.isBlank()) {
            refreshTokenStore.revoke(refreshPlain);
        }

        cookieHelper.deleteCookie("accessToken");
        cookieHelper.deleteCookie("refreshToken");

        return ResponseEntity.ok(new RsData<>(HttpStatus.OK, "로그아웃 되었습니다."));
    }

    @GetMapping("/me")
    public ResponseEntity<RsData<MemberDto>> me(
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        Member member = memberService.getById(securityUser.getId());

        String presignedUrl = s3Uploader.generatePresignedUrl(member.getProfileImgUrl());
        return ResponseEntity.ok(new RsData<>(HttpStatus.OK, "현재 회원 정보입니다.", new MemberDto(member, presignedUrl)));
    }

    @PatchMapping("/me")
    public ResponseEntity<RsData<MemberDto>> updateMe(
            @AuthenticationPrincipal SecurityUser securityUser,
            @Valid @RequestPart(value = "reqBody") MemberUpdateReqBody reqBody,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage
    ) {
        Member member = memberService.updateMember(securityUser.getId(), reqBody, profileImage);

        String presignedUrl = s3Uploader.generatePresignedUrl(member.getProfileImgUrl());
        return ResponseEntity.ok(new RsData<>(HttpStatus.OK, "회원 정보가 수정되었습니다.", new MemberDto(member, presignedUrl)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RsData<SimpleMemberDto>> getMember(
            @PathVariable Long id
    ) {
        Member member = memberService.getById(id);
        return ResponseEntity.ok(new RsData<>(HttpStatus.OK, "회원 정보입니다.", new SimpleMemberDto(member)));
    }

    @GetMapping("/check-nickname")
    public ResponseEntity<RsData<MemberNicknameResBody>> checkNickname(
            @RequestParam String nickname
    ){
        boolean isDuplicated = memberService.existsByNickname(nickname);
        return ResponseEntity.ok(new RsData<>(HttpStatus.OK, "닉네임 중복 확인 완료", new MemberNicknameResBody(isDuplicated)));
    }

    @PostMapping("/send-code")
    public ResponseEntity<RsData<MemberSendCodeResBody>> sendVerificationCode(
            @RequestBody @Valid MemberSendCodeReqBody reqBody
    ) {
        LocalDateTime expiresIn = memberService.sendEmailVerificationCode(reqBody.email());
        return ResponseEntity.ok(new RsData<>(HttpStatus.OK, "이메일 인증이 발송되었습니다.", new MemberSendCodeResBody(expiresIn)));
    }

    @PostMapping("/verify-code")
    public ResponseEntity<RsData<MemberVerifyResBody>> verifyCode(
            @RequestBody @Valid MemberVerifyReqBody reqBody
    ) {
        emailService.verifyCode(reqBody.email(), reqBody.code());
        return ResponseEntity.ok(new RsData<>(HttpStatus.OK, "이메일 인증이 완료되었습니다.", new MemberVerifyResBody(true)));
    }

    @GetMapping("/{id}/reviews/summary")
    public ResponseEntity<RsData<String>> summarizeReviews(@PathVariable Long id) {
        String body = reviewSummaryService.summarizeMemberReviews(id);

        return ResponseEntity.ok(new RsData<>(HttpStatus.OK, HttpStatus.OK.name(), body));
    }
}
