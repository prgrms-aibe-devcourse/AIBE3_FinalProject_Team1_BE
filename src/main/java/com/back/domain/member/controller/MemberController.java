package com.back.domain.member.controller;

import com.back.domain.member.dto.MemberDto;
import com.back.domain.member.dto.MemberJoinReqBody;
import com.back.domain.member.dto.MemberLoginReqBody;
import com.back.domain.member.dto.SimpleMemberDto;
import com.back.domain.member.entity.Member;
import com.back.domain.member.service.AuthTokenService;
import com.back.domain.member.service.MemberService;
import com.back.domain.member.service.RefreshTokenStore;
import com.back.global.exception.ServiceException;
import com.back.global.rsData.RsData;
import com.back.global.security.SecurityUser;
import com.back.global.web.CookieHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController implements MemberApi{
    private final MemberService memberService;
    private final AuthTokenService authTokenService;
    private final RefreshTokenStore refreshTokenStore;
    private final CookieHelper cookieHelper;

    @PostMapping
    public ResponseEntity<RsData<MemberDto>> join(
            @Valid @RequestBody MemberJoinReqBody reqBody
    ) {
        Member member =memberService.join(reqBody);

        return ResponseEntity.status(201).body(new RsData<>(HttpStatus.CREATED, "회원가입 되었습니다.", new MemberDto(member)));
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

        return ResponseEntity.ok(new RsData<>(HttpStatus.OK, "로그인 되었습니다.", new MemberDto(member)));
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

        return ResponseEntity.ok(new RsData<>(HttpStatus.OK, "현재 회원 정보입니다.", new MemberDto(member)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RsData<SimpleMemberDto>> getMember(
            @PathVariable Long id
    ) {
        Member member = memberService.getById(id);
        return ResponseEntity.ok(new RsData<>(HttpStatus.OK, "회원 정보입니다.", new SimpleMemberDto(member)));
    }
}
