package com.back.domain.notification.service;

import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
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
import com.back.global.sse.EmitterRepository;
import com.back.standard.util.page.PagePayload;
import com.back.standard.util.page.PageUt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@Slf4j
public class NotificationService {

    private final MemberRepository memberRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationQueryRepository notificationQueryRepository;
    private final ReservationQueryRepository reservationQueryRepository;
    private final List<NotificationDataMapper<? extends NotificationData>> mappers;
    private final Map<NotificationType.GroupType, Function<List<Long>, Map<Long, ?>>> batchLoaders = new HashMap<>();
    private final EmitterRepository emitterRepository;
    private static final Long TIMEOUT = 60L * 1000 * 60; // 1시간

    public NotificationService(
            MemberRepository memberRepository,
            NotificationRepository notificationRepository,
            NotificationQueryRepository notificationQueryRepository,
            ReservationQueryRepository reservationQueryRepository,
            List<NotificationDataMapper<? extends NotificationData>> mappers,
            EmitterRepository emitterRepository
    ) {
        this.memberRepository = memberRepository;
        this.notificationRepository = notificationRepository;
        this.notificationQueryRepository = notificationQueryRepository;
        this.reservationQueryRepository = reservationQueryRepository;
        this.mappers = mappers;
        this.emitterRepository = emitterRepository;
        setBatchLoaders();
    }

    private void setBatchLoaders() {
        batchLoaders.put(NotificationType.GroupType.RESERVATION, targetIds ->
                reservationQueryRepository.findWithPostAndAuthorByIds(targetIds)
                        .stream().collect(Collectors.toMap(Reservation::getId, r -> r))
        );
    }

    public SseEmitter subscribe(Long memberId) {
        String emitterId = memberId + "_" + System.currentTimeMillis();
        SseEmitter emitter = createAndSaveEmitter(memberId, emitterId);

        sendInitialEvent(memberId, emitterId, emitter);
        registerEmitterCallbacks(memberId, emitterId, emitter);

        return emitter;
    }

    private SseEmitter createAndSaveEmitter(Long memberId, String emitterId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT);
        emitterRepository.save(memberId, emitterId, emitter);
        return emitter;
    }

    private void sendInitialEvent(Long memberId, String emitterId, SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event()
                    .id(emitterId)
                    .data("connected"));
        } catch (Exception e) {
            log.error("SSE 발행 중 예외 발생: memberId={}, emitterId={}, error={}",
                    memberId, emitterId, e.getMessage(), e);
        }
    }

    private void registerEmitterCallbacks(Long memberId, String emitterId, SseEmitter emitter) {
        emitter.onCompletion(() -> emitterRepository.deleteEmitter(memberId, emitterId));
        emitter.onTimeout(() -> emitterRepository.deleteEmitter(memberId, emitterId));
        emitter.onError(e -> emitterRepository.deleteEmitter(memberId, emitterId));
    }

    @Transactional
    public void saveAndSendNotification(Long targetMemberId, NotificationType type, Long targetId) {
        Member member = memberRepository.findById(targetMemberId)
                .orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND,
                        "멤버(%d)를 찾을 수 없습니다.".formatted(targetMemberId)));

        Notification notification = Notification.create(type, targetId, member);
        Notification saved = notificationRepository.save(notification);

        NotificationResBody<?> dto = EntityToResBody(saved);

        sendNotification(targetMemberId, dto);
    }

    public void sendNotification(Long targetMemberId, NotificationResBody<? extends NotificationData> message) {
        Map<String, SseEmitter> emitters = emitterRepository.findEmittersByMemberId(targetMemberId);

        emitters.forEach((emitterId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .id(emitterId)
                        .data(message));
            } catch (Exception e) {
                emitterRepository.deleteEmitter(targetMemberId, emitterId);
            }
        });
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

    private NotificationResBody<?> EntityToResBody(Notification notification) {
        Map<NotificationType.GroupType, Map<Long, ?>> loaded = loadEntitiesByGroup(List.of(notification));
        List<NotificationResBody<? extends NotificationData>> bodies = mapToResBody(List.of(notification), loaded);
        return bodies.get(0);
    }
}

