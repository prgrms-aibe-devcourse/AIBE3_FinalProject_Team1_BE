package com.back.domain.report.common.validator;


import com.back.domain.post.repository.PostRepository;
import com.back.domain.report.common.ReportType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostReportValidator implements ReportValidator {

    private final PostRepository postRepository;

    @Override
    public boolean validateTargetId(ReportType reportType, Long targetId) {
        boolean exists = postRepository.existsById(targetId);
        if (!exists) {
            log.error("신고 대상(Post) 없음(targetId: {})", targetId);
        }

        return reportType == ReportType.POST && exists;
    }
}
