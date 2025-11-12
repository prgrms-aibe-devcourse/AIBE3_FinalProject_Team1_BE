package com.back.domain.report.report.dto;

import com.back.domain.report.report.common.ReportType;
import com.back.domain.report.report.entity.Report;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ReportResBody(
        @Schema(description = "등록된 신고 고유 ID")
        Long reportId,

        @Schema(description = "등록된 신고 유형(게시글 or 사용자 or 후기)")
        ReportType reportType,

        @Schema(description = "등록된 신고 대상(게시글 or 사용자 or 후기) 고유 ID")
        Long targetId,

        @Schema(description = "등록된 신고 내용")
        String comment,

        @Schema(description = "신고 등록 요청자 고유 ID")
        Long authorId,

        @Schema(description = "신고가 등록된 시간")
        LocalDateTime createdAt
) {
    public static ReportResBody from(Report report) {
        return ReportResBody.builder()
                            .reportId(report.getId())
                            .reportType(report.getReportType())
                            .targetId(report.getTargetId())
                            .comment(report.getComment())
                            .authorId(report.getMember().getId())
                            .createdAt(report.getCreatedAt())
                            .build();
    }
}
