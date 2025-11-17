package com.back.domain.chat.service;

import com.back.domain.chat.dto.*;
import com.back.domain.chat.entity.ChatMember;
import com.back.domain.chat.entity.ChatMessage;
import com.back.domain.chat.entity.ChatRoom;
import com.back.domain.chat.repository.ChatMessageRepository;
import com.back.domain.chat.repository.ChatQueryRepository;
import com.back.domain.chat.repository.ChatRoomRepository;
import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import com.back.domain.post.entity.Post;
import com.back.domain.post.repository.PostRepository;
import com.back.global.exception.ServiceException;
import com.back.standard.util.page.PagePayload;
import com.back.standard.util.page.PageUt;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatWebsocketService chatWebsocketService;
    private final MemberRepository memberRepository;
    private final PostRepository postRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatQueryRepository chatQueryRepository;
    private final RedisTemplate<String, String> redisTemplate;


    @Transactional
    public CreateChatRoomResBody createOrGetChatRoom(Long postId, Long memberId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "존재하지 않는 게시글입니다."));
        Member host = post.getAuthor();

        if (host.getId().equals(memberId)) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "본인과 채팅방을 만들 수 없습니다.");
        }

        Optional<Long> existingRoom = chatQueryRepository.getChatRoomId(postId, memberId);
        if (existingRoom.isPresent()) {
            Long roomId = existingRoom.get();
            return new CreateChatRoomResBody("이미 존재하는 채팅방입니다.", roomId);
        }

        Member guest = memberRepository.findById(memberId).orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."));
        ChatRoom chatRoom = ChatRoom.create(post, host, guest);

        chatRoomRepository.save(chatRoom);

        OtherMemberDto otherMemberDto = new OtherMemberDto(
                guest.getId(),
                guest.getNickname(),
                guest.getProfileImgUrl()
        );

        NewRoomNotiDto newRoom = new NewRoomNotiDto(
                chatRoom.getId(),
                chatRoom.getCreatedAt(),
                new ChatPostDto(post.getTitle()),
                otherMemberDto,
                null,
                null,
                0
        );

        chatWebsocketService.notify(
                host.getId(),
                new ChatNotiDto("NEW_ROOM", newRoom)
        );

        return new CreateChatRoomResBody("채팅방이 생성되었습니다.", chatRoom.getId());
    }

    public PagePayload<ChatRoomListDto> getMyChatRooms(Long memberId, Pageable pageable, String keyword) {
        Page<ChatRoomListDto> chatRooms = chatQueryRepository.getMyChatRooms(memberId, pageable, keyword);

        Page<ChatRoomListDto> enrichedPage = chatRooms.map(dto -> {
            String key = "unread:" + memberId + ":" + dto.id();
            String unreadStr = redisTemplate.opsForValue().get(key);
            Integer unreadCount = unreadStr == null ? 0 : Integer.parseInt(unreadStr);

            return dto.withUnreadCount(unreadCount);
        });

        return PageUt.of(enrichedPage);
    }

    public ChatRoomDto getChatRoom(Long chatRoomId, Long memberId) {
        ChatRoom chatRoom = chatQueryRepository.getChatRoom(chatRoomId)
                .orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "존재하지 않는 채팅방입니다."));

        Member otherMember = getMember(memberId, chatRoom);

        Post post = chatRoom.getPost();
        ChatPostDto chatPostDto = new ChatPostDto(post.getTitle());
        OtherMemberDto otherMemberDto = new OtherMemberDto(
                otherMember.getId(),
                otherMember.getNickname(),
                otherMember.getProfileImgUrl()
        );

        return new ChatRoomDto(chatRoom.getId(), chatRoom.getCreatedAt(), chatPostDto, otherMemberDto);
    }

    private Member getMember(Long memberId, ChatRoom chatRoom) {
        Member currentMember = null;
        Member otherMember = null;

        for (ChatMember chatMember : chatRoom.getChatMembers()) {
            Member member = chatMember.getMember();
            if (member.getId() == memberId) {
                currentMember = member;
            } else {
                otherMember = member;
            }
        }

        if (currentMember == null) {
            throw new ServiceException(HttpStatus.FORBIDDEN, "해당 채팅방에 접근할 수 없습니다.");
        }

        if (otherMember == null) {
            throw new ServiceException(HttpStatus.NOT_FOUND, "채팅 상대 정보가 없습니다.");
        }

        return otherMember;
    }

    public PagePayload<ChatMessageDto> getChatMessageList(Long chatRoomId, Long memberId, Pageable pageable) {

        boolean isMember = chatQueryRepository.isMemberInChatRoom(chatRoomId, memberId);
        if (!isMember) {
            throw new ServiceException(HttpStatus.FORBIDDEN, "채팅방이 존재하지 않거나 접근 권한이 없습니다.");
        }

        Page<ChatMessageDto> chatMessages = chatQueryRepository.getChatMessages(chatRoomId, memberId, pageable);

        return PageUt.of(chatMessages);
    }

    @Transactional
    public void saveMessage(Long chatRoomId, SendChatMessageDto body, Long memberId) {
        String content = body.content();

        ChatMember chatMember = chatQueryRepository.findChatMember(chatRoomId, memberId)
                .orElseThrow(() -> new ServiceException(HttpStatus.FORBIDDEN, "채팅방이 존재하지 않거나 접근 권한이 없습니다."));


        ChatMessage chatMessage = ChatMessage.create(content, chatRoomId, chatMember.getId());
        chatMessageRepository.save(chatMessage);

        Long otherMemberId = chatQueryRepository.findOtherMemberId(chatRoomId, memberId)
                .orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "채팅 상대를 찾을 수 없습니다."));

        String key = "unread:" + otherMemberId + ":" + chatRoomId;
        redisTemplate.opsForValue().increment(key);

        chatMember.getChatRoom().updateLastMessage(chatMessage.getContent(), chatMessage.getCreatedAt());

        ChatMessageDto chatMessageDto = new ChatMessageDto(
                chatMessage.getId(),
                memberId,
                chatMessage.getContent(),
                chatMessage.getCreatedAt()
        );

        chatWebsocketService.broadcastMessage(chatRoomId, chatMessageDto);

        chatWebsocketService.notify(
                otherMemberId,
                new ChatNotiDto("NEW_MESSAGE", NewMessageNotiDto.from(chatRoomId, chatMessageDto))
        );

    }

    @Transactional
    public void markAsRead(Long chatRoomId, Long memberId, Long lastMessageId) {
        ChatMember chatMember = chatQueryRepository.findChatMember(chatRoomId, memberId)
                .orElseThrow(() -> new ServiceException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."));

        chatMember.updateLastReadMessageId(lastMessageId);

        String key = "unread:" + memberId + ":" + chatRoomId;
        redisTemplate.delete(key);
    }

}
