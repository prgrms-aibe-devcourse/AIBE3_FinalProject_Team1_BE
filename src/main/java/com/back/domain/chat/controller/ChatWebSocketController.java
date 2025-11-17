package com.back.domain.chat.controller;

import com.back.domain.chat.dto.SendChatMessageDto;
import com.back.domain.chat.service.ChatService;
import com.back.global.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;

    @MessageMapping("/chat/{chatRoomId}")
    public void sendMessage(
            @DestinationVariable Long chatRoomId,
            @Payload SendChatMessageDto body,
            Authentication authentication
    ) {

        SecurityUser user = (SecurityUser) authentication.getPrincipal();

        chatService.saveMessage(chatRoomId, body, user.getId());
    }
}
