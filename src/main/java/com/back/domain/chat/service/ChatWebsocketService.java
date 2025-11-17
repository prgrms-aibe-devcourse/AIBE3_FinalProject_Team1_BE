package com.back.domain.chat.service;

import com.back.domain.chat.dto.ChatMessageDto;
import com.back.domain.chat.dto.ChatNotiDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatWebsocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcastMessage(Long chatRoomId, ChatMessageDto message) {
        messagingTemplate.convertAndSend("/sub/chat/" + chatRoomId, message);
    }

    public void notify(Long memberId, ChatNotiDto notification) {
        messagingTemplate.convertAndSend(
                "/sub/notifications/" + memberId,
                notification
        );
    }
}
