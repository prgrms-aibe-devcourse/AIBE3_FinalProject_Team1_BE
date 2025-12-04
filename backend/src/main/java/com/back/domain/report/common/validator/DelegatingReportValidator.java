package com.back.domain.report.common.validator;

import com.back.domain.report.common.ReportType;
import com.back.global.exception.ServiceException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Primary
@Component
@RequiredArgsConstructor
public class DelegatingReportValidator implements ReportValidator {

    @Getter
    private final List<ReportValidator> reportValidators;

    @Override
    public boolean validateTargetId(ReportType reportType, Long targetId, Long reporterId) {
        for (ReportValidator validator : reportValidators) {
            if (validator.validateTargetId(reportType, targetId, reporterId)) {
                return true;
            }
        }

        throw new ServiceException(HttpStatus.BAD_REQUEST, "지원하지 않는 신고 유형입니다.");
    }
}
