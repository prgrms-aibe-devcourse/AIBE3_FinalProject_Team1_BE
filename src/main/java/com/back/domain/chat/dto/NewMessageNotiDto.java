package com.back.domain.chat.dto;

import java.time.LocalDateTime;

public record NewMessageNotiDto(
        Long chatRoomId,
        Long messageId,
        Long authorId,
        String content,
        LocalDateTime createdAt
) {
    public static NewMessageNotiDto from(Long chatRoomId, ChatMessageDto chatMessageDto) {
        return new NewMessageNotiDto(
                chatRoomId,
                chatMessageDto.id(),
                chatMessageDto.authorId(),
                chatMessageDto.content(),
                chatMessageDto.createdAt()
        );
    }
}
