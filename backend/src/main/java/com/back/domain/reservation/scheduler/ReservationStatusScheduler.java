package com.back.domain.reservation.scheduler;

import com.back.domain.reservation.scheduler.job.ReservationStatusJob;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class ReservationStatusScheduler {

    @Bean
    public JobDetail reservationStatusJobDetail() {
        return JobBuilder.newJob(ReservationStatusJob.class)
                .withIdentity("reservationStatusJob", "reservation")
                .withDescription("예약 상태 자동 업데이트 작업")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger reservationStatusTrigger(JobDetail reservationStatusJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(reservationStatusJobDetail)
                .withIdentity("reservationStatusTrigger", "reservation")
                .withSchedule(
                        CronScheduleBuilder.dailyAtHourAndMinute(17, 0)
                )
                .build();
    }

    @Bean
    public CommandLineRunner registerReservationStatusJob(
            Scheduler scheduler,
            JobDetail reservationStatusJobDetail,
            Trigger reservationStatusTrigger
    ) {
        return args -> {
            JobKey jobKey = reservationStatusJobDetail.getKey();

            if (!scheduler.checkExists(jobKey)) {
                scheduler.scheduleJob(reservationStatusJobDetail, reservationStatusTrigger);
                log.info("✅ 스케줄러 등록 완료: 예약 상태 자동 업데이트 작업이 매일 오후 5시에 실행됩니다.");
            } else {
                scheduler.rescheduleJob(
                        reservationStatusTrigger.getKey(),
                        reservationStatusTrigger
                );
                log.info("🔄 스케줄러 재등록 완료: 예약 상태 자동 업데이트 작업이 갱신되었습니다.");
            }
        };
    }
}
