package com.back.domain.member.controller;

import com.back.domain.member.dto.MemberBannedResBody;
import com.back.domain.member.dto.MemberDto;
import com.back.domain.member.service.MemberService;
import com.back.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/adm/members")
public class MemberAdmController implements MemberAdmApi {
    private final MemberService memberService;

    @PatchMapping("/{id}/ban")
    public ResponseEntity<RsData<MemberBannedResBody>> banMember(
            @PathVariable Long id
    ) {
        MemberBannedResBody memberBannedResBody = memberService.banMember(id);
        RsData<MemberBannedResBody> response = new RsData<>(HttpStatus.OK, "회원이 제재되었습니다.", memberBannedResBody);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/unban")
    public ResponseEntity<RsData<MemberBannedResBody>> unbanMember(
            @PathVariable Long id
    ) {
        MemberBannedResBody memberBannedResBody = memberService.unbanMember(id);
        RsData<MemberBannedResBody> response = new RsData<>(HttpStatus.OK, "회원 제재가 해제되었습니다.", memberBannedResBody);
        return ResponseEntity.ok(response);
    }
}
