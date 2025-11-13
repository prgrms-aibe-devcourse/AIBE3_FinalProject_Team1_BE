package com.back.domain.report.dto;

import com.back.domain.report.common.ReportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReportReqBody(
        @NotNull ReportType reportType,
        @NotNull Long targetId,
        @NotBlank String comment
) {
}
