package com.back.domain.chat.controller;

import com.back.domain.chat.dto.*;
import com.back.global.rsData.RsData;
import com.back.global.security.SecurityUser;
import com.back.standard.util.page.PagePayload;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Chat API", description = "채팅 기능 관련 API, 인증 필요")
public interface ChatApi {

    @Operation(summary = "채팅방 생성 또는 조회 API", description = "특정 게시글에 대해 채팅방이 존재하면 가져오고, 없으면 새로 생성합니다.")
    ResponseEntity<RsData<CreateChatRoomResBody>> createOrGetChatRoom(
            CreateChatRoomReqBody reqBody,
            @AuthenticationPrincipal SecurityUser securityUser
    );

    @Operation(summary = "내 채팅방 목록 조회 API", description = "내가 속한 채팅방 목록을 조회합니다.")
    ResponseEntity<RsData<PagePayload<ChatRoomListDto>>> getMyChatRooms(
            Pageable pageable,
            String keyword,
            @AuthenticationPrincipal SecurityUser securityUser
    );

    @Operation(summary = "채팅방 상세 조회 API", description = "특정 채팅방의 상세 정보를 조회합니다.")
    public ResponseEntity<RsData<ChatRoomDto>> getChatRoom(
            @PathVariable Long id,
            @AuthenticationPrincipal SecurityUser securityUser
    );

    @Operation(summary = "채팅방 내 메세지 조회 API", description = "특정 채팅방의 메세지를 조회합니다.")
    public ResponseEntity<RsData<PagePayload<ChatMessageDto>>> getChatRoomMessages(
            Pageable pageable,
            @PathVariable Long id,
            @AuthenticationPrincipal SecurityUser securityUser
    );

    @Operation(summary = "채팅방 내 메세지 조회 API", description = "특정 채팅방 내 특정 메세지까지 모두 읽음 처리합니다.")
    public ResponseEntity<RsData<Void>> markAsRead(
            @PathVariable Long id,
            @RequestParam Long lastMessageId,
            @AuthenticationPrincipal SecurityUser securityUser
    );
}
