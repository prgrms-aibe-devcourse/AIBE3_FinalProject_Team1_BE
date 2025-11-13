package com.back.domain.report.report.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.report.report.common.ReportType;
import com.back.domain.report.report.common.validator.ReportValidator;
import com.back.domain.report.report.dto.ReportReqBody;
import com.back.domain.report.report.dto.ReportResBody;
import com.back.domain.report.report.entity.Report;
import com.back.domain.report.report.repository.ReportQueryRepository;
import com.back.domain.report.report.repository.ReportRepository;
import com.back.global.exception.ServiceException;
import com.back.standard.util.page.PagePayload;
import com.back.standard.util.page.PageUt;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportValidator reportValidator;
    private final ReportRepository reportRepository;
    private final MemberRepository memberRepository;
    private final ReportQueryRepository reportQueryRepository;

    @Transactional
    public ReportResBody postReport(ReportReqBody reportReqBody, long reporterId) {
        reportValidator.validateTargetId(reportReqBody.reportType(), reportReqBody.targetId());

        Member reporter = memberRepository.findById(reporterId)
                                          .orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."));

        Report savedReport = reportRepository.save(Report.builder()
                                                         .comment(reportReqBody.comment())
                                                         .targetId(reportReqBody.targetId())
                                                         .reportType(reportReqBody.reportType())
                                                         .member(reporter)
                                                         .build());
        return ReportResBody.from(savedReport);
    }

    public PagePayload<ReportResBody> getReports(Pageable pageable, ReportType reportType) {
        Page<ReportResBody> pages = reportQueryRepository.getReports(pageable, reportType);
        return PageUt.of(pages);
    }
}
