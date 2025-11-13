package com.back.domain.report.dto;

import com.back.domain.report.common.ReportType;
import com.back.domain.report.entity.Report;

import java.time.LocalDateTime;

public record ReportResBody(
        Long reportId,
        ReportType reportType,
        Long targetId,
        String comment,
        Long authorId,
        LocalDateTime createdAt
) {
    public static ReportResBody from(Report report) {
        return new ReportResBody(
                report.getId(),
                report.getReportType(),
                report.getTargetId(),
                report.getComment(),
                report.getMember().getId(),
                report.getCreatedAt()
        );
    }
}
