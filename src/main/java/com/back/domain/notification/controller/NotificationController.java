package com.back.domain.notification.controller;

import com.back.domain.notification.dto.NotificationUnreadResBody;
import com.back.domain.notification.service.NotificationService;
import com.back.global.rsData.RsData;
import com.back.global.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController implements NotificationApi {

    private final NotificationService notificationService;

    @GetMapping("/unread")
    public ResponseEntity<RsData<NotificationUnreadResBody>> hasUnread(@AuthenticationPrincipal SecurityUser securityUser) {
        NotificationUnreadResBody notificationUnreadResBody = notificationService.hasUnread(securityUser.getId());
        RsData<NotificationUnreadResBody> response = new RsData<>(HttpStatus.OK, "읽지 않은 알림 존재 여부", notificationUnreadResBody);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<RsData<Void>> updateAllToRead(@AuthenticationPrincipal SecurityUser securityUser) {
        notificationService.updateAllToRead(securityUser.getId());
        RsData<Void> response = new RsData<>(HttpStatus.OK, "모든 알림 읽음 처리 성공");
        return ResponseEntity.ok(response);
    }
}
