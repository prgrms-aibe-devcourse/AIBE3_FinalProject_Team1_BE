package com.back.domain.report.common.validator;

import com.back.domain.member.repository.MemberRepository;
import com.back.domain.report.common.ReportType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserReportValidator implements ReportValidator {

    private final MemberRepository memberRepository;

    @Override
    public boolean validateTargetId(ReportType reportType, Long targetId) {
        boolean exists = memberRepository.existsById(targetId);
        if (!exists) {
            log.error("신고 대상(Member) 없음(targetId: {})", targetId);
        }

        return reportType == ReportType.USER && exists;
    }
}
