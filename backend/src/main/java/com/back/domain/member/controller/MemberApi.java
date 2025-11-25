package com.back.domain.member.controller;

import com.back.domain.member.dto.*;
import com.back.global.rsData.RsData;
import com.back.global.security.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

public interface MemberApi {
    @Operation(summary = "회원가입", description = "비밀번호 8자리 이상, 프로필 이미지는 계정 수정에서 추가 가능")
    ResponseEntity<RsData<MemberDto>> join(
            @Valid @RequestBody MemberJoinReqBody reqBody
    );
    @Operation(summary = "로그인", description = "비밀번호 8자리 이상")
    ResponseEntity<RsData<MemberDto>> login(
            @Valid @RequestBody MemberLoginReqBody reqBody
    );

    @Operation(summary = "로그아웃", description = "토큰 삭제 포함")
    ResponseEntity<RsData<Void>> logout();

    @Operation(summary = "내 정보 조회", description = "로그인한 회원의 정보를 조회합니다.")
    ResponseEntity<RsData<MemberDto>> me(
            @AuthenticationPrincipal SecurityUser securityUser
    );

    @Operation(summary = "내 정보 수정", description = "로그인한 회원의 정보를 수정합니다.")
    ResponseEntity<RsData<MemberDto>> updateMe(
            @AuthenticationPrincipal SecurityUser securityUser,
            @Valid @RequestPart(value = "reqBody") MemberUpdateReqBody reqBody,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage
    );

    @Operation(summary = "사용자 조회(일반 회원)", description = "ID로 회원 정보를 조회합니다.")
    ResponseEntity<RsData<SimpleMemberDto>> getMember(
            @PathVariable Long id
    );

    @Operation(summary = "닉네임 중복 체크", description = "닉네임이 중복되는지 확인합니다.")
    ResponseEntity<RsData<MemberNicknameResBody>> checkNickname(
            @RequestParam String nickname
    );

    @Operation(summary = "인증 코드 전송", description = "이메일로 인증 코드를 전송합니다.")
    public ResponseEntity<RsData<MemberSendCodeResBody>> sendVerificationCode(
            @RequestBody @Valid MemberSendCodeReqBody reqBody
    );

    @Operation(summary = "인증 코드 검증", description = "이메일과 인증 코드를 검증합니다.")
    public ResponseEntity<RsData<MemberVerifyResBody>> verifyCode(
            @RequestBody @Valid MemberVerifyReqBody reqBody
    );
}
