package com.back.domain.chat.controller;

import com.back.domain.chat.dto.ChatRoomDto;
import com.back.domain.chat.dto.CreateChatRoomReqBody;
import com.back.domain.chat.dto.CreateChatRoomResBody;
import com.back.domain.chat.service.ChatService;
import com.back.global.rsData.RsData;
import com.back.global.security.SecurityUser;
import com.back.standard.util.page.PagePayload;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    public ResponseEntity<RsData<PagePayload<ChatRoomDto>>> getMyChatRooms(
            @PageableDefault(size = 10, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        PagePayload<ChatRoomDto> myChatRooms = chatService.getMyChatRooms(securityUser.getId(), pageable, keyword);
        return ResponseEntity.ok(new RsData<>(HttpStatus.OK, "내 채팅방 목록",  myChatRooms));
    }

    @GetMapping("/{chatRoomId}")
    public ResponseEntity<RsData<ChatRoomDto>> getChatRoom(
            @PathVariable Long chatRoomId,
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        ChatRoomDto chatRoom = chatService.getChatRoom(chatRoomId, securityUser.getId());
        return ResponseEntity.ok(new RsData<>(HttpStatus.OK, "채팅방 정보", chatRoom));
    }
}
