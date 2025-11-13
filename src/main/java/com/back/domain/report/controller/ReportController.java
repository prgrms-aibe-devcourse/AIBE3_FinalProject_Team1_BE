package com.back.domain.report.controller;

import com.back.domain.report.dto.ReportReqBody;
import com.back.domain.report.dto.ReportResBody;
import com.back.domain.report.service.ReportService;
import com.back.global.rsData.RsData;
import com.back.global.security.SecurityUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reports")
public class ReportController implements ReportApi {

    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<RsData<ReportResBody>> postReport(@Valid @RequestBody ReportReqBody reportReqBody,
                                                            @AuthenticationPrincipal SecurityUser securityUser) {

        ReportResBody data = reportService.postReport(reportReqBody, securityUser.getId());

        HttpStatus created = HttpStatus.CREATED;
        RsData<ReportResBody> body = new RsData<>(created, created.name(), data);

        return ResponseEntity.status(created).body(body);
    }
}
