package com.back.domain.report.common.validator;

import com.back.domain.report.common.ReportType;
import com.back.domain.review.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewReportValidator implements ReportValidator {

    private final ReviewRepository reviewRepository;

    @Override
    public boolean validateTargetId(ReportType reportType, Long targetId) {
        boolean exists = reviewRepository.existsById(targetId);
        if (!exists) {
            log.error("신고 대상(Review) 없음(targetId: {})", targetId);
        }

        return reportType == ReportType.REVIEW && exists;
    }
}
