package com.back.domain.report.scheduler;

import com.back.domain.report.scheduler.job.ReportAutoBanJob;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class ReportAutoBanScheduler {

    @Bean
    public JobDetail reportAutoBanJobDetail() {
        return JobBuilder.newJob(ReportAutoBanJob.class)
                .withIdentity("ReportAutoBanJob", "report")
                .withDescription("신고 5건 이상 컨텐츠 자동 차단 작업")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger reportAutoBanTrigger(JobDetail reportAutoBanJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(reportAutoBanJobDetail)
                .withIdentity("ReportAutoBanTrigger", "report")
                .withSchedule(
                        CronScheduleBuilder.dailyAtHourAndMinute(17, 0)
//                            SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(30).repeatForever()
                )
                .build();
    }

    @Bean
    public CommandLineRunner registerReportAutoBanJob(
            Scheduler scheduler,
            JobDetail reportAutoBanJobDetail,
            Trigger reportAutoBanTrigger
    ) {
        return args -> {
            JobKey jobKey = reportAutoBanJobDetail.getKey();

            if (!scheduler.checkExists(jobKey)) {
                scheduler.scheduleJob(reportAutoBanJobDetail, reportAutoBanTrigger);
                log.info("✅ 스케줄러 등록 완료: 신고 자동 제재 작업이 매일 오후 5시에 실행됩니다.");
            } else {
                scheduler.rescheduleJob(
                        reportAutoBanTrigger.getKey(),
                        reportAutoBanTrigger
                );
                log.info("🔄 스케줄러 재등록 완료: 신고 자동 제재 작업이 갱신되었습니다.");
            }
        };
    }
}
