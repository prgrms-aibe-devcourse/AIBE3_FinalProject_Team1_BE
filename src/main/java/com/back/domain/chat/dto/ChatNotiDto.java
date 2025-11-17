package com.back.domain.chat.dto;

public record ChatNotiDto(
        String type, //"NEW_ROOM" | "NEW_MESSAGE"
        Object payload
) {
}
