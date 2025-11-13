package com.back.domain.notification.common;

import java.time.LocalDateTime;

public record ReservationNotificationData(
        PostInfo postInfo,
        ReservationInfo reservationInfo
) implements NotificationData {

    public record ReservationInfo(
            Long id,
            Author author,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            String cancelReason,
            String rejectReason
    ) {}

    public record PostInfo(
            Long id,
            String title,
            Author author
    ) {}
}

