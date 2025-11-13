package com.back.domain.notification.mapper;

import com.back.domain.notification.common.NotificationData;
import com.back.domain.notification.common.NotificationType;
import com.back.domain.notification.entity.Notification;

public interface NotificationDataMapper<T extends NotificationData> {
    boolean supports(NotificationType type);
    T map(Object data, Notification notification);
}
