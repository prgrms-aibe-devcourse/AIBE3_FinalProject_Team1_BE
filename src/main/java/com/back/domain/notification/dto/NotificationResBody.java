package com.back.domain.notification.dto;

import com.back.domain.notification.common.NotificationData;
import com.back.domain.notification.common.NotificationType;

import java.time.LocalDateTime;

public record NotificationResBody<T extends NotificationData>(
    NotificationType notificationType,
    LocalDateTime createdAt,
    T data
) {
}
