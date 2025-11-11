package com.back.domain.report.report.controller;

import com.back.ControllerTestSupport;
import com.back.domain.report.report.common.ReportType;
import com.back.domain.report.report.dto.ReportReqBody;
import com.back.domain.report.report.dto.ReportResBody;
import com.back.domain.report.report.service.ReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ReportControllerTest extends ControllerTestSupport {

    ReportService reportService = mock(ReportService.class);

    @Override
    protected Object initController() {
        return new ReportController(reportService);
    }

    @Test
    @DisplayName("신고를 등록하면 등록된 신고 정보를 반환한다.")
    void postReport() throws Exception {
        //given
        ReportReqBody request = new ReportReqBody(ReportType.USER, 3L, "홍보 목적 게시글 신고");
        ReportResBody response = ReportResBody.builder()
                                              .reportId(1L)
                                              .reportType(ReportType.USER)
                                              .targetId(3L)
                                              .comment("홍보 목적 사용자")
                                              .authorId(1L)
                                              .createdAt(LocalDateTime.now())
                                              .build();
        given(reportService.postReport(any(ReportReqBody.class), anyLong())).willReturn(response);

        //when
        mockMvc.perform(post("/api/v1/reports")
                       .contentType(MediaType.APPLICATION_JSON_VALUE)
                       .content(creatJson(request)))
        //then
               .andExpectAll(
                       handler().handlerType(ReportController.class),
                       handler().methodName("postReport"),
                       status().isOk(),
                       jsonPath("$.msg").value("신고가 등록되었습니다."),
                       jsonPath("$.data.reportId").value(response.reportId()),
                       jsonPath("$.data.reportType").value(response.reportType().name()),
                       jsonPath("$.data.targetId").value(response.targetId()),
                       jsonPath("$.data.comment").value(response.comment()),
                       jsonPath("$.data.authorId").value(response.authorId()),
                       jsonPath("$.data.createdAt").exists()
               );
        verify(reportService).postReport(any(ReportReqBody.class), anyLong());
    }
}