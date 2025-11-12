package com.back.domain.report.report.controller;

import com.back.domain.report.report.dto.ReportReqBody;
import com.back.domain.report.report.dto.ReportResBody;
import com.back.domain.report.report.service.ReportService;
import com.back.global.security.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reports")
@Tag(name = "Report API", description = "신고 기능 관련 API")
public class ReportController {

    private final ReportService reportService;

    @Operation(
            summary = "신고 등록 API",
            description = "게시글 or 사용자 or 후기에 대한 신고를 등록합니다.",
            responses = {@ApiResponse(responseCode = "200", description = "신고 등록 성공", content = @Content(schema = @Schema(implementation = ReportResBody.class)))}
    )
    @PostMapping
    public ResponseEntity<ReportResBody> postReport(@Valid @RequestBody ReportReqBody reportReqBody,
                                                    @AuthenticationPrincipal SecurityUser securityUser) {
        ReportResBody response = reportService.postReport(reportReqBody, securityUser.getId());
        return ResponseEntity.ok(response);
    }
}
