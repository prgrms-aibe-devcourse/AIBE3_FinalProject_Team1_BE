package com.back.domain.chat.dto;

import java.time.LocalDateTime;

public record ChatRoomListDto(
        Long id,
        LocalDateTime createdAt,
        ChatPostDto post,
        OtherMemberDto otherMember,

        String lastMessage,
        LocalDateTime lastMessageTime,
        Integer unreadCount
) {
    public ChatRoomListDto withUnreadCount(Integer unreadCount) {
        return new ChatRoomListDto(
                this.id,
                this.createdAt,
                this.post,
                this.otherMember,
                this.lastMessage,
                this.lastMessageTime,
                unreadCount
        );
    }
}

