package com.back.domain.member.controller;

import com.back.domain.member.dto.MemberBannedResBody;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Member Admin API", description = "사용자 관리자 API, 관리자 인증 필요")
public interface MemberAdmApi {
    @Operation(summary = "사용자 제재 API", description = "id에 해당하는 사용자를 제재합니다.")
    ResponseEntity<RsData<MemberBannedResBody>> banMember(@PathVariable Long id);

    @Operation(summary = "사용자 제재 해제 API", description = "id에 해당하는 사용자의 제재를 해제합니다.")
    ResponseEntity<RsData<MemberBannedResBody>> unbanMember(@PathVariable Long id);
}
