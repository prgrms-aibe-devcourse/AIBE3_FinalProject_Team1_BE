package com.back.domain.notification.entity;

import com.back.domain.member.entity.Member;
import com.back.domain.notification.common.NotificationType;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notification")
public class Notification extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NotificationType type;

    @Column(name = "target_id", nullable = true)
    private Long targetId;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    public void updateToRead() {
        isRead = true;
    }

    public static Notification create(NotificationType type, Long targetId, Long memberId) {
        Notification notification = new Notification();
        notification.type = type;
        notification.targetId = targetId;
        notification.memberId = memberId;
        notification.isRead = false;

        return notification;
    }
}
