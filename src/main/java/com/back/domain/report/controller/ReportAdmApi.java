package com.back.domain.report.controller;

import com.back.domain.report.common.ReportType;
import com.back.domain.report.dto.ReportResBody;
import com.back.global.rsData.RsData;
import com.back.standard.util.page.PagePayload;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

@Tag(name = "Report API", description = "신고 기능 관련 API")
public interface ReportAdmApi {

    @Operation(summary = "신고 목록 조회 API (관리자용)", description = "관리자에 한해 신고 목록을 조회합니다.")
    ResponseEntity<RsData<PagePayload<ReportResBody>>> getReports(Pageable pageable, ReportType reportType);
}
