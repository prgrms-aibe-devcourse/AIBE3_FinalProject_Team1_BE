package com.back.domain.report.repository;

import com.back.domain.report.common.ReportType;
import com.back.domain.report.dto.ReportResBody;
import com.back.domain.report.entity.Report;
import com.back.global.queryDsl.CustomQuerydslRepositorySupport;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import static com.back.domain.member.entity.QMember.member;
import static com.back.domain.report.entity.QReport.report;

@Repository
public class ReportQueryRepository extends CustomQuerydslRepositorySupport {

    public ReportQueryRepository() {
        super(Report.class);
    }

    public Page<ReportResBody> getReports(Pageable pageable, ReportType reportType) {
        return applyPagination(pageable,
                contentQuery -> contentQuery
                        .select(Projections.constructor(
                                ReportResBody.class,
                                report.id,
                                report.reportType,
                                report.targetId,
                                report.comment,
                                member.id,
                                report.createdAt
                        ))
                        .from(report)
                        .join(report.member, member)
                        .where(reportTypeEq(reportType)),
                countQuery -> countQuery.select(report.count())
                                        .from(report)
                                        .where(reportTypeEq(reportType))
        );
    }

    private BooleanExpression reportTypeEq(ReportType reportType) {
        return reportType != null ? report.reportType.eq(reportType) : null;
    }
}
