package com.back.domain.notification.repository;

import com.back.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Boolean existsByMemberIdAndIsReadFalse(Long memberId);

    @EntityGraph(attributePaths = {"member"})
    Optional<Notification> findNotificationWithMemberById(Long id);
}
