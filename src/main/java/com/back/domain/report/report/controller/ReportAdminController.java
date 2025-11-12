package com.back.domain.report.report.controller;

import com.back.domain.report.report.common.ReportType;
import com.back.domain.report.report.dto.ReportResBody;
import com.back.domain.report.report.service.ReportService;
import com.back.standard.util.page.PagePayload;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.data.domain.Sort.Direction.DESC;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/adm/reports")
@Tag(name = "Report API", description = "신고 기능 관련 API")
public class ReportAdminController {

    private final ReportService reportService;

    @Operation(
            summary = "신고 목록 조회 API (관리자용)",
            description = "관리자에 한해 신고 목록을 조회합니다.",
            responses = {@ApiResponse(responseCode = "200", description = "신고 목록 조회 성공", content = @Content(schema = @Schema(implementation = ReportResBody.class)))}
    )
    @GetMapping
    public ResponseEntity<PagePayload<ReportResBody>> getReports(@ParameterObject @PageableDefault(sort = "id", direction = DESC) Pageable pageable,
                                                                 @RequestParam(value = "reportType", required = false) ReportType reportType) {
        PagePayload<ReportResBody> response = reportService.getReports(pageable, reportType);
        return ResponseEntity.ok(response);
    }
}
