package com.back.domain.notification.common;

import java.time.LocalDate;

public record ReservationNotificationData(
        PostInfo postInfo,
        ReservationInfo reservationInfo
) implements NotificationData {

    public record ReservationInfo(
            Long id,
            Author author,
            LocalDate startDate,
            LocalDate endDate,
            String cancelReason,
            String rejectReason
    ) {}

    public record PostInfo(
            Long id,
            String title,
            Author author
    ) {}
}

