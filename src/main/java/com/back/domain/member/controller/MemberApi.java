package com.back.domain.member.controller;

import com.back.domain.member.dto.MemberDto;
import com.back.domain.member.dto.MemberJoinReqBody;
import com.back.domain.member.dto.MemberLoginReqBody;
import com.back.global.security.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

public interface MemberApi {
    @Operation(summary = "회원가입", description = "비밀번호 8자리 이상, 프로필 이미지는 계정 수정에서 추가 가능")
    ResponseEntity<String> join(
            @Valid @RequestBody MemberJoinReqBody reqBody
    );
    @Operation(summary = "로그인", description = "비밀번호 8자리 이상")
    ResponseEntity<MemberDto> login(
            @Valid @RequestBody MemberLoginReqBody reqBody
    );

    @Operation(summary = "로그아웃", description = "토큰 삭제 포함")
    ResponseEntity<String> logout();

    @Operation(summary = "내 정보 조회", description = "로그인한 회원의 정보를 조회합니다.")
    ResponseEntity<MemberDto> me(
            @AuthenticationPrincipal SecurityUser securityUser
    );
}
