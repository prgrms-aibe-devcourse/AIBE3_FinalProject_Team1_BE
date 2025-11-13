package com.back.domain.notification.service;

import com.back.domain.notification.dto.NotificationUnreadResBody;
import com.back.domain.notification.repository.NotificationQueryRepository;
import com.back.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
