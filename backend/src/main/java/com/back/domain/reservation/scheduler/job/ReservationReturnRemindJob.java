package com.back.domain.reservation.scheduler.job;

import com.back.domain.notification.common.NotificationType;
import com.back.domain.notification.service.NotificationService;
import com.back.domain.reservation.entity.Reservation;
import com.back.domain.reservation.repository.ReservationQueryRepository;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class ReservationReturnRemindJob implements Job {

    @Autowired
    private ReservationQueryRepository reservationQueryRepository;

    @Autowired
    private NotificationService notificationService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Long reservationId = context.getMergedJobDataMap().getLong("reservationId");

        Reservation reservation = reservationQueryRepository
                .findByIdWithPostAndAuthor(reservationId)
                .orElse(null);

        if (reservation == null) {
            log.warn("예약 정보를 찾을 수 없음 - reservationId: {}", reservationId);
            return;
        }

        try {
            Long memberId = reservation.getAuthor().getId();

            notificationService.saveAndSendNotification(memberId, NotificationType.REMIND_RETURN_DUE, reservationId);
            log.info("알림 전송 완료 - reservationId: {}, memberId: {}",
                    reservationId, memberId);

        } catch (Exception e) {
            log.error("알림 전송 실패 - reservationId: {}", reservationId, e);
        }
    }
}
