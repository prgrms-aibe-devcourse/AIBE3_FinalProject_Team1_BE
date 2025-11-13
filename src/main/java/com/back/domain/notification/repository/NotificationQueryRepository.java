package com.back.domain.notification.repository;

import com.back.domain.notification.entity.Notification;
import com.back.global.queryDsl.CustomQuerydslRepositorySupport;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;

import static com.back.domain.notification.entity.QNotification.notification;

@Repository
public class NotificationQueryRepository extends CustomQuerydslRepositorySupport {

    public NotificationQueryRepository() {
        super(Notification.class);
    }

    public void markAllAsReadByMemberId(Long memberId) {
        JPAQueryFactory query = getQueryFactory();

        query.update(notification)
                .set(notification.isRead, true)
                .where(notification.member.id.eq(memberId)
                        .and(notification.isRead.eq(false)))
                .execute();
    }
}
