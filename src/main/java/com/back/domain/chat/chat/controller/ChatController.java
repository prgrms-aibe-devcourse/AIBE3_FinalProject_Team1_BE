package com.back.domain.chat.chat.controller;

import com.back.domain.chat.chat.dto.ChatRoomDto;
import com.back.domain.chat.chat.dto.CreateChatRoomReqBody;
import com.back.domain.chat.chat.dto.CreateChatRoomResBody;
import com.back.domain.chat.chat.service.ChatService;
import com.back.global.security.SecurityUser;
import com.back.standard.util.page.PagePayload;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<CreateChatRoomResBody> createOrGetChatRoom(
            @RequestBody CreateChatRoomReqBody reqBody,
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        CreateChatRoomResBody body = chatService.createOrGetChatRoom(
                reqBody.postId(),
                securityUser.getId()
        );

        return ResponseEntity.ok(body);
    }

    // TODO : 페이지네이션 & 검색 기능 추가
    @GetMapping
    public ResponseEntity<PagePayload<ChatRoomDto>> getMyChatRooms(
            @PageableDefault(size = 10, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        PagePayload<ChatRoomDto> myChatRooms = chatService.getMyChatRooms(securityUser.getId(), pageable, keyword);
        return ResponseEntity.ok(myChatRooms);
    }
}
