package com.back.domain.reservation.scheduler;

import com.back.domain.reservation.scheduler.job.ReservationReturnRemindJob;
import com.back.standard.util.quartz.QuartzUt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationRemindScheduler {

    private final Scheduler scheduler;

    public void scheduleReturnReminder(Long reservationId, LocalDateTime reservationEndAt) {

        JobDetail jobDetail = JobBuilder.newJob(ReservationReturnRemindJob.class)
                .withIdentity("returnReminderJob-" + reservationId)
                .usingJobData("reservationId", reservationId)
                .build();

        LocalDateTime runAt = QuartzUt.reminderAt10AmOneDayBefore(reservationEndAt);

        Date runDate = Date.from(runAt.atZone(ZoneId.systemDefault()).toInstant());

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("returnReminderTrigger-" + reservationId)
                .startAt(runDate)
                .forJob(jobDetail)
                .build();

        try {
            scheduler.scheduleJob(jobDetail, trigger);
            log.info("반납 리마인더 스케줄링 완료 - reservationId: {}, runAt: {}", reservationId, runAt);
        } catch (SchedulerException e) {
            log.error("반납 리마인더 스케줄링 실패 - reservationId: {}, runAt: {}", reservationId, runAt, e);
        }
    }
}

