package com.back.domain.chat.dto;

import java.time.LocalDateTime;

public record NewRoomNotiDto(
        Long id,
        LocalDateTime createdAt,
        ChatPostDto post,
        OtherMemberDto otherMember,

        String lastMessage,
        LocalDateTime lastMessageTime,
        Integer unreadCount
) {
}
