package com.back.domain.chat.controller;

import com.back.domain.chat.dto.*;
import com.back.domain.chat.service.ChatService;
import com.back.global.rsData.RsData;
import com.back.global.security.SecurityUser;
import com.back.standard.util.page.PagePayload;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class ChatController implements ChatApi{
    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<RsData<CreateChatRoomResBody>> createOrGetChatRoom(
            @RequestBody CreateChatRoomReqBody reqBody,
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        CreateChatRoomResBody body = chatService.createOrGetChatRoom(reqBody.postId(), securityUser.getId());
        return ResponseEntity.ok(new RsData<>(HttpStatus.OK, body.message(), body));
    }

    @GetMapping
    public ResponseEntity<RsData<PagePayload<ChatRoomListDto>>> getMyChatRooms(
            @PageableDefault(size = 10, page = 0) Pageable pageable,
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        PagePayload<ChatRoomListDto> myChatRooms = chatService.getMyChatRooms(securityUser.getId(), pageable, keyword);
        return ResponseEntity.ok(new RsData<>(HttpStatus.OK, "내 채팅방 목록",  myChatRooms));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RsData<ChatRoomDto>> getChatRoom(
            @PathVariable("id") Long chatRoomId,
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        ChatRoomDto chatRoom = chatService.getChatRoom(chatRoomId, securityUser.getId());
        return ResponseEntity.ok(new RsData<>(HttpStatus.OK, "채팅방 정보", chatRoom));
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<RsData<PagePayload<ChatMessageDto>>> getChatRoomMessages(
            @PageableDefault(size = 10, page = 0) Pageable pageable,
            @PathVariable("id") Long chatRoomId,
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        PagePayload<ChatMessageDto> chatMessages = chatService.getChatMessageList(chatRoomId, securityUser.getId(), pageable);
        return ResponseEntity.ok(new RsData<>(HttpStatus.OK, "해당 채팅방 내 메세지 목록", chatMessages));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<RsData<Void>> markAsRead(
            @PathVariable("id") Long chatRoomId,
            @RequestParam Long lastMessageId,
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        chatService.markAsRead(chatRoomId, securityUser.getId(), lastMessageId);
        return ResponseEntity.ok(new RsData<>(HttpStatus.OK, "읽음 처리 완료", null));
    }
}
