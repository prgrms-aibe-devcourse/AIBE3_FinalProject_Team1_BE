package com.back.domain.notification.controller;

import com.back.domain.notification.dto.NotificationUnreadResBody;
import com.back.global.rsData.RsData;
import com.back.global.security.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Notification API", description = "알림 API, 인증 정보 필요")
public interface NotificationApi {

    @Operation(summary = "읽지 않은 알림 여부 조회 API", description = "읽지 않은 알림 여부 응답")
    ResponseEntity<RsData<NotificationUnreadResBody>> hasUnread(@AuthenticationPrincipal SecurityUser securityUser);

    @Operation(summary = "알림 모두 읽음 처리 API", description = "알림 모두 읽음 처리")
    ResponseEntity<RsData<Void>> updateAllToRead(@AuthenticationPrincipal SecurityUser securityUser);

    @Operation(summary = "단일 알림 읽음 처리 API", description = "id에 해당하는 알림 읽음 처리")
    ResponseEntity<RsData<Void>> updateToRead(
            @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable("id") Long notificationId
    );
}
