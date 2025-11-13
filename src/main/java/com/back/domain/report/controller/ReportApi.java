package com.back.domain.report.controller;

import com.back.domain.report.dto.ReportReqBody;
import com.back.domain.report.dto.ReportResBody;
import com.back.global.rsData.RsData;
import com.back.global.security.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Report API", description = "신고 기능 관련 API")
public interface ReportApi {

    @Operation(summary = "신고 등록 API", description = "게시글 or 사용자 or 후기에 대한 신고를 등록합니다.")
    ResponseEntity<RsData<ReportResBody>> postReport(ReportReqBody reportReqBody, SecurityUser securityUser);
}
