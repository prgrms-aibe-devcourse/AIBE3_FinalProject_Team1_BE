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
public class Notification extends BaseEntity {

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private Long targetId;

    private Boolean isRead = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    public void updateToRead() {
        isRead = true;
    }
}
