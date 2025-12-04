package com.back.domain.chat.service;

import com.back.domain.chat.dto.*;
import com.back.domain.chat.entity.ChatMember;
import com.back.domain.chat.entity.ChatMessage;
import com.back.domain.chat.entity.ChatRoom;
import com.back.domain.chat.pubsub.publisher.ChatMessagePublisher;
import com.back.domain.chat.pubsub.publisher.ChatNotificationPublisher;
import com.back.domain.chat.repository.*;
import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import com.back.domain.post.entity.Post;
import com.back.domain.post.repository.PostRepository;
import com.back.global.exception.ServiceException;
import com.back.global.s3.S3Uploader;
import com.back.standard.util.page.PagePayload;
import com.back.standard.util.page.PageUt;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final MemberRepository memberRepository;
    private final PostRepository postRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomQueryRepository chatRoomQueryRepository;
    private final ChatMessageQueryRepository chatMessageQueryRepository;
    private final StringRedisTemplate redisTemplate;
    private final ChatMessagePublisher chatMessagePublisher;
    private final ChatNotificationPublisher chatNotificationPublisher;
    private final S3Uploader s3;

    private static final String NOTI_NEW_ROOM = "NEW_ROOM";
    private static final String NOTI_NEW_MESSAGE = "NEW_MESSAGE";

    @Transactional
    public CreateChatRoomResBody createOrGetChatRoom(Long postId, Long memberId) {

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "존재하지 않는 게시글입니다."));

        Long hostId = post.getAuthor().getId();
        validateNotSelfChat(hostId, memberId);

        Optional<Long> existingRoom = chatRoomQueryRepository.getChatRoomId(postId, memberId);
        if (existingRoom.isPresent()) {
            return new CreateChatRoomResBody("이미 존재하는 채팅방입니다.", existingRoom.get());
        }

        Member guest = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."));

        ChatRoom chatRoom = ChatRoom.create(postId, post.getTitle());
        chatRoomRepository.save(chatRoom);

        chatMemberRepository.save(ChatMember.create(chatRoom.getId(), hostId));
        chatMemberRepository.save(ChatMember.create(chatRoom.getId(), memberId));

        notifyNewRoom(hostId, chatRoom, post, guest);

        return new CreateChatRoomResBody("채팅방이 생성되었습니다.", chatRoom.getId());
    }

    private void validateNotSelfChat(Long hostId, Long guestId) {
        if (hostId.equals(guestId)) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "본인과 채팅방을 만들 수 없습니다.");
        }
    }

    private void notifyNewRoom(Long hostId, ChatRoom chatRoom, Post post, Member guest) {

        OtherMemberDto guestDto = new OtherMemberDto(
                guest.getId(),
                guest.getNickname(),
                s3.generatePresignedUrl(guest.getProfileImgUrl())
        );

        NewRoomNotiDto newRoom = new NewRoomNotiDto(
                chatRoom.getId(),
                chatRoom.getCreatedAt(),
                new ChatPostDto(post.getTitle()),
                guestDto,
                null,
                null,
                0
        );

        chatNotificationPublisher.publish(
                hostId,
                new ChatNotiDto(NOTI_NEW_ROOM, newRoom)
        );
    }

    @Transactional(readOnly = true)
    public PagePayload<ChatRoomListDto> getMyChatRooms(Long memberId, Pageable pageable, String keyword) {

        Page<ChatRoomListDto> chatRooms = chatRoomQueryRepository.getMyChatRooms(memberId, pageable, keyword);

        Page<ChatRoomListDto> enrichedPage = chatRooms.map(dto -> {
            String key = unreadKey(memberId, dto.id());
            Integer unreadCount = getUnreadCount(key);

            String presignedUrl = s3.generatePresignedUrl(dto.otherMember().profileImgUrl());
            return dto.withUnreadCount(unreadCount, presignedUrl);
        });

        return PageUt.of(enrichedPage);
    }

    @Transactional(readOnly = true)
    public ChatRoomDto getChatRoom(Long chatRoomId, Long memberId) {

        ChatRoom chatRoom = chatRoomQueryRepository.getChatRoom(chatRoomId)
                .orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "존재하지 않는 채팅방입니다."));

        if (!chatRoomQueryRepository.isMemberInChatRoom(chatRoomId, memberId)) {
            throw new ServiceException(HttpStatus.FORBIDDEN, "해당 채팅방에 접근할 수 없습니다.");
        }

        Member otherMember = chatRoomQueryRepository.findOtherMember(chatRoomId, memberId)
                .orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "채팅 상대 정보가 없습니다."));

        return new ChatRoomDto(
                chatRoom.getId(),
                chatRoom.getCreatedAt(),
                new ChatPostDto(chatRoom.getPostTitleSnapshot()),
                new OtherMemberDto(
                        otherMember.getId(),
                        otherMember.getNickname(),
                        s3.generatePresignedUrl(otherMember.getProfileImgUrl())
                )
        );
    }

    @Transactional(readOnly = true)
    public PagePayload<ChatMessageDto> getChatMessageList(Long chatRoomId, Long memberId, Pageable pageable) {

        if (!chatRoomQueryRepository.isMemberInChatRoom(chatRoomId, memberId)) {
            throw new ServiceException(HttpStatus.FORBIDDEN, "채팅방이 존재하지 않거나 접근 권한이 없습니다.");
        }

        return PageUt.of(chatMessageQueryRepository.getChatMessages(chatRoomId, memberId, pageable));
    }

    @Transactional
    public void saveMessage(Long chatRoomId, SendChatMessageDto body, Long memberId) {

        ChatMember chatMember = chatRoomQueryRepository.findChatMember(chatRoomId, memberId)
                .orElseThrow(() -> new ServiceException(HttpStatus.FORBIDDEN, "채팅방이 존재하지 않거나 접근 권한이 없습니다."));

        ChatMessage chatMessage = ChatMessage.create(body.content(), chatRoomId, chatMember.getId());
        chatMessageRepository.save(chatMessage);

        Long otherMemberId = chatRoomQueryRepository.findOtherMemberId(chatRoomId, memberId)
                .orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "채팅 상대를 찾을 수 없습니다."));

        redisTemplate.opsForValue().increment(unreadKey(otherMemberId, chatRoomId));

        updateChatRoomLastMessage(chatRoomId, chatMessage);

        publishMessageAndNotification(chatRoomId, memberId, otherMemberId, chatMessage);
    }

    private void updateChatRoomLastMessage(Long chatRoomId, ChatMessage chatMessage) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "존재하지 않는 채팅방입니다."));

        chatRoom.updateLastMessage(chatMessage.getContent(), chatMessage.getCreatedAt());
    }

    private void publishMessageAndNotification(Long chatRoomId, Long senderId, Long receiverId, ChatMessage chatMessage) {

        ChatMessageDto dto = new ChatMessageDto(
                chatMessage.getId(),
                senderId,
                chatMessage.getContent(),
                chatMessage.getCreatedAt()
        );

        chatMessagePublisher.publish(chatRoomId, dto);

        chatNotificationPublisher.publish(
                receiverId,
                new ChatNotiDto(NOTI_NEW_MESSAGE, NewMessageNotiDto.from(chatRoomId, dto))
        );
    }

    @Transactional
    public void markAsRead(Long chatRoomId, Long memberId, Long lastMessageId) {

        ChatMember chatMember = chatRoomQueryRepository.findChatMember(chatRoomId, memberId)
                .orElseThrow(() -> new ServiceException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."));

        chatMember.updateLastReadMessageId(lastMessageId);

        redisTemplate.delete(unreadKey(memberId, chatRoomId));
    }

    private Integer getUnreadCount(String key) {
        String unreadStr = redisTemplate.opsForValue().get(key);
        if (unreadStr == null) return 0;

        try {
            return Integer.parseInt(unreadStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String unreadKey(Long memberId, Long chatRoomId) {
        return "unread:" + memberId + ":" + chatRoomId;
    }
}
