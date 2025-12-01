package com.back.domain.reservation.scheduler;

import com.back.domain.reservation.scheduler.job.ReservationReturnRemindJob;
import org.quartz.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReservationRemindScheduler {

    @Bean
    public JobDetail reservationReturnJobDetail() {
        return JobBuilder.newJob(ReservationReturnRemindJob.class)
                .withIdentity("reservationReturnJob", "reservation")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger reservationReturnTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(reservationReturnJobDetail())
                .withIdentity("reservationReturnTrigger", "reservation")
                .withSchedule(
                        CronScheduleBuilder.dailyAtHourAndMinute(10, 0)
                )
                .build();
    }

    @Bean
    public CommandLineRunner registerReservationJobs(
            Scheduler scheduler,
            JobDetail reservationReturnJobDetail,
            Trigger reservationReturnTrigger
    ) {
        return args -> {
            JobKey jobKey = reservationReturnJobDetail.getKey();

            if(!scheduler.checkExists(jobKey)) {
                scheduler.scheduleJob(reservationReturnJobDetail, reservationReturnTrigger);
            } else {
                scheduler.rescheduleJob(
                        reservationReturnTrigger.getKey(),
                        reservationReturnTrigger
                );
            }
        };
    }
}