package com.back.domain.notification.repository;

import com.back.domain.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Boolean existsByMemberIdAndIsReadFalse(Long memberId);

    Optional<Notification> findNotificationWithMemberById(Long id);

    Page<Notification> findAllByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :dateTime AND n.isRead = true")
    int deleteOldReadNotifications(@Param("dateTime") LocalDateTime dateTime);
}
