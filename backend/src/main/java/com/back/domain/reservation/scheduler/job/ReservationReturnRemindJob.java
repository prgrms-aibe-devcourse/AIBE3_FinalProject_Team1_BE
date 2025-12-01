package com.back.domain.reservation.scheduler.job;

import com.back.domain.notification.common.NotificationType;
import com.back.domain.notification.service.NotificationService;
import com.back.domain.reservation.common.ReservationStatus;
import com.back.domain.reservation.entity.Reservation;
import com.back.domain.reservation.repository.ReservationQueryRepository;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@DisallowConcurrentExecution
public class ReservationReturnRemindJob implements Job {

    @Autowired
    private ReservationQueryRepository reservationQueryRepository;

    @Autowired
    private NotificationService notificationService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        List<Reservation> targets =
                reservationQueryRepository.findAllByEndAtAndStatus(
                        tomorrow,
                        ReservationStatus.RENTING
                );

        if (targets.isEmpty()) {
            log.info("[REMIND JOB] 알림 대상 없음");
            return;
        }

        for (Reservation reservation : targets) {
            try {
                Long memberId = reservation.getAuthor().getId();
                notificationService.saveAndSendNotification(
                        memberId,
                        NotificationType.REMIND_RETURN_DUE,
                        reservation.getId()
                );

                log.info("[REMIND JOB] 알림 전송 : reservationId = {}, memberId = {}", reservation.getId(), memberId);
            } catch (Exception e) {
                log.error("[REMIND JOB] 알림 실패 : reservationId = {}", reservation.getId(), e);
            }
        }
    }
}