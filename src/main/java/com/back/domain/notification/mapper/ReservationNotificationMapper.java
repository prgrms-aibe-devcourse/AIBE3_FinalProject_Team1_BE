package com.back.domain.notification.mapper;

import com.back.domain.notification.common.Author;
import com.back.domain.notification.common.NotificationType;
import com.back.domain.notification.common.ReservationNotificationData;
import com.back.domain.notification.entity.Notification;
import com.back.domain.reservation.entity.Reservation;
import org.springframework.stereotype.Component;

@Component
public class ReservationNotificationMapper implements NotificationDataMapper<ReservationNotificationData> {

    @Override
    public boolean supports(NotificationType type) {
        return type.getGroupType() == NotificationType.GroupType.RESERVATION;
    }

    @Override
    public ReservationNotificationData map(Object entity, Notification notification) {
        Reservation reservation = (Reservation) entity;
        return new ReservationNotificationData(
                new ReservationNotificationData.PostInfo(
                        reservation.getPost().getId(),
                        reservation.getPost().getTitle(),
                        new Author(reservation.getPost().getAuthor().getId(), reservation.getPost().getAuthor().getNickname())
                ),
                new ReservationNotificationData.ReservationInfo(
                        reservation.getId(),
                        new Author(reservation.getAuthor().getId(), reservation.getAuthor().getNickname()),
                        reservation.getReservationStartAt(),
                        reservation.getReservationEndAt(),
                        reservation.getCancelReason(),
                        reservation.getRejectReason()
                )
        );
    }
}
