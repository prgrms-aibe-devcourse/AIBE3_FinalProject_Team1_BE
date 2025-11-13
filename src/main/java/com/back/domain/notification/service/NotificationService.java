package com.back.domain.notification.service;

import com.back.domain.notification.common.NotificationData;
import com.back.domain.notification.common.NotificationType;
import com.back.domain.notification.dto.NotificationResBody;
import com.back.domain.notification.dto.NotificationUnreadResBody;
import com.back.domain.notification.entity.Notification;
import com.back.domain.notification.mapper.NotificationDataMapper;
import com.back.domain.notification.repository.NotificationQueryRepository;
import com.back.domain.notification.repository.NotificationRepository;
import com.back.domain.reservation.entity.Reservation;
import com.back.domain.reservation.repository.ReservationQueryRepository;
import com.back.global.exception.ServiceException;
import com.back.standard.util.page.PagePayload;
import com.back.standard.util.page.PageUt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationQueryRepository notificationQueryRepository;
    private final ReservationQueryRepository reservationQueryRepository;
    private final List<NotificationDataMapper<? extends NotificationData>> mappers;
    private final Map<NotificationType.GroupType, Function<List<Long>, Map<Long, ?>>> batchLoaders = new HashMap<>();

    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationQueryRepository notificationQueryRepository,
            ReservationQueryRepository reservationQueryRepository,
            List<NotificationDataMapper<? extends NotificationData>> mappers
    ) {
        this.notificationRepository = notificationRepository;
        this.notificationQueryRepository = notificationQueryRepository;
        this.reservationQueryRepository = reservationQueryRepository;
        this.mappers = mappers;
        setBatchLoaders();
    }

    private void setBatchLoaders() {
        batchLoaders.put(NotificationType.GroupType.RESERVATION, targetIds ->
                reservationQueryRepository.findWithPostAndAuthorByIds(targetIds)
                        .stream().collect(Collectors.toMap(Reservation::getId, r -> r))
        );
    }

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

    public PagePayload<NotificationResBody<? extends NotificationData>> getNotifications(Long memberId, Pageable pageable) {

        Page<Notification> notificationsPage = notificationRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId, pageable);
        List<Notification> notifications = notificationsPage.getContent();

        Map<NotificationType.GroupType, Map<Long, ?>> loadedEntities = loadEntitiesByGroup(notifications);

        List<NotificationResBody<? extends NotificationData>> resBodyList = mapToResBody(notifications, loadedEntities);

        Page<NotificationResBody<? extends NotificationData>> page =
                new PageImpl<>(resBodyList, pageable, notificationsPage.getTotalElements());

        return PageUt.of(page);
    }

    private Map<NotificationType.GroupType, Map<Long, ?>> loadEntitiesByGroup(List<Notification> notifications) {
        Map<NotificationType.GroupType, List<Long>> groupedTargetIds = notifications.stream()
                .collect(Collectors.groupingBy(
                        n -> n.getType().getGroupType(),
                        Collectors.mapping(Notification::getTargetId, Collectors.toList())
                ));

        Map<NotificationType.GroupType, Map<Long, ?>> loadedEntities = new HashMap<>();
        for (Map.Entry<NotificationType.GroupType, List<Long>> entry : groupedTargetIds.entrySet()) {
            NotificationType.GroupType groupType = entry.getKey();
            List<Long> targetIds = entry.getValue();
            Function<List<Long>, Map<Long, ?>> loader = batchLoaders.get(groupType);
            if (loader != null) {
                loadedEntities.put(groupType, loader.apply(targetIds));
            }
        }
        return loadedEntities;
    }

    private List<NotificationResBody<? extends NotificationData>> mapToResBody(
            List<Notification> notifications,
            Map<NotificationType.GroupType, Map<Long, ?>> loadedEntities
    ) {
        List<NotificationResBody<? extends NotificationData>> resBodyList = new ArrayList<>();
        for (Notification notification : notifications) {
            for (NotificationDataMapper<? extends NotificationData> mapper : mappers) {
                if (mapper.supports(notification.getType())) {

                    Map<Long, ?> entityMap = loadedEntities.get(notification.getType().getGroupType());
                    Object entity = entityMap != null ? entityMap.get(notification.getTargetId()) : null;

                    NotificationData data = mapper.map(entity, notification);
                    resBodyList.add(new NotificationResBody<>(
                            notification.getId(),
                            notification.getType(),
                            notification.getCreatedAt(),
                            notification.getIsRead(),
                            data
                    ));
                }
            }
        }
        return resBodyList;
    }
}

