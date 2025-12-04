package com.back.domain.report.common.validator;

import com.back.domain.report.common.ReportType;
import com.back.domain.review.entity.Review;
import com.back.domain.review.repository.ReviewRepository;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewReportValidator implements ReportValidator {

    private final ReviewRepository reviewRepository;

    @Override
    public boolean validateTargetId(ReportType reportType, Long targetId, Long reporterId) {
        if (reportType != ReportType.REVIEW) {
            return false;
        }

        Review review = reviewRepository.findById(targetId)
                                        .orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "존재하지 않는 후기입니다."));

        if (review.getReservation().getAuthor().getId().equals(reporterId)) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "본인의 후기는 신고할 수 없습니다.");
        }

        return true;
    }
}
