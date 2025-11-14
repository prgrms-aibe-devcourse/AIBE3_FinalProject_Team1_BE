package com.back.domain.chat.dto;

public record SendChatMessageDto(
        Long chatRoomId,
        String content
) {
}
