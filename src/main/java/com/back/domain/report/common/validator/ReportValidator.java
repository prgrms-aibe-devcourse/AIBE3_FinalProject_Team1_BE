package com.back.domain.report.common.validator;

import com.back.domain.report.common.ReportType;

public interface ReportValidator {
    boolean validateTargetId(ReportType reportType, Long targetId);
}
