package com.back.domain.report.common.validator;

import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import com.back.domain.report.common.ReportType;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberReportValidator implements ReportValidator {

    private final MemberRepository memberRepository;

    @Override
    public boolean validateTargetId(ReportType reportType, Long targetId, Long reporterId) {
        if (reportType != ReportType.MEMBER) {
            return false;
        }
        Member member = memberRepository.findById(targetId)
                                        .orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."));

        if (member.getId().equals(reporterId)) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "본인은 신고할 수 없습니다.");
        }

        return true;
    }
}
