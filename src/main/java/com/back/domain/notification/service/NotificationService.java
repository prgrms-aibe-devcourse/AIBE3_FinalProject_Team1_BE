package com.back.domain.notification.service;

import com.back.domain.notification.dto.NotificationUnreadResBody;
import com.back.domain.notification.entity.Notification;
import com.back.domain.notification.repository.NotificationQueryRepository;
import com.back.domain.notification.repository.NotificationRepository;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationQueryRepository notificationQueryRepository;

    public NotificationUnreadResBody hasUnread(Long memberId) {
        Boolean hasUnread = notificationRepository.existsByMemberIdAndIsReadFalse(memberId);
        return new NotificationUnreadResBody(hasUnread);
    }

    @Transactional
    public void updateAllToRead(Long memberId) {
        notificationQueryRepository.markAllAsReadByMemberId(memberId);
    }

    @Transactional
    public void updateToRead(Long MemberId, Long notificationId) {
        Notification notification = notificationRepository.findNotificationWithMemberById(notificationId).orElseThrow(
                () -> new ServiceException(HttpStatus.NOT_FOUND, "%d번 알림을 찾을 수 없습니다.".formatted(notificationId))
        );

        if (!Objects.equals(notification.getMember().getId(), MemberId)) {
            throw new ServiceException(HttpStatus.FORBIDDEN, "해당 알림 읽음처리 권한이 없습니다.");
        }

        notification.updateToRead();
    }
}
