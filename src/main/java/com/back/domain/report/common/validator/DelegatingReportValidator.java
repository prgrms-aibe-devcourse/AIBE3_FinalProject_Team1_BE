package com.back.domain.report.common.validator;

import com.back.domain.report.common.ReportType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

@Primary
@Component
@RequiredArgsConstructor
public class DelegatingReportValidator implements ReportValidator {

    @Getter
    private final List<ReportValidator> reportValidators;

    @Override
    public boolean validateTargetId(ReportType reportType, Long targetId) {
        for (ReportValidator validator : reportValidators) {
            if (validator.validateTargetId(reportType, targetId)) {
                return true;
            }
        }

        throw new IllegalArgumentException("지원하지 않는 대상: " + reportType);
    }
}
