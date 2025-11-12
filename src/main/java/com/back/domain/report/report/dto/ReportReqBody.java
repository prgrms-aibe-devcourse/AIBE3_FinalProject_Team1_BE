package com.back.domain.report.report.dto;

import com.back.domain.report.report.common.ReportType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReportReqBody(
        @Schema(description = "신고 유형(게시글 or 사용자 or 후기)")
        @NotNull
        ReportType reportType,

        @Schema(description = "신고 대상(게시글 or 사용자 or 후기) 고유 ID")
        @NotNull
        Long targetId,

        @Schema(description = "신고 내용")
        @NotBlank
        String comment
) {
}
