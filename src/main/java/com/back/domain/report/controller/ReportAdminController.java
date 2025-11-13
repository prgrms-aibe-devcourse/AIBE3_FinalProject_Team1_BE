package com.back.domain.report.controller;

import com.back.domain.report.common.ReportType;
import com.back.domain.report.dto.ReportResBody;
import com.back.domain.report.service.ReportService;
import com.back.global.rsData.RsData;
import com.back.standard.util.page.PagePayload;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.data.domain.Sort.Direction.DESC;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/adm/reports")
public class ReportAdminController implements ReportAdmApi {

    private final ReportService reportService;

    @GetMapping
    public ResponseEntity<RsData<PagePayload<ReportResBody>>> getReports(@ParameterObject @PageableDefault(sort = "id", direction = DESC) Pageable pageable,
                                                                         @RequestParam(value = "reportType", required = false) ReportType reportType) {

        PagePayload<ReportResBody> data = reportService.getReports(pageable, reportType);
        RsData<PagePayload<ReportResBody>> body = new RsData<>(HttpStatus.OK, HttpStatus.OK.name(), data);

        return ResponseEntity.ok(body);
    }
}
