package com.back.domain.chat.service;

import com.back.domain.chat.dto.ChatPostDto;
import com.back.domain.chat.dto.ChatRoomDto;
import com.back.domain.chat.dto.CreateChatRoomResBody;
import com.back.domain.chat.dto.OtherMemberDto;
import com.back.domain.chat.entity.ChatMember;
import com.back.domain.chat.entity.ChatRoom;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {
    private final MemberRepository memberRepository;
    private final PostRepository postRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatQueryRepository chatQueryRepository;

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

        return new CreateChatRoomResBody("채팅방이 생성되었습니다.", chatRoom.getId());
    }

    public PagePayload<ChatRoomDto> getMyChatRooms(Long memberId, Pageable pageable, String keyword) {
        Page<ChatRoomDto> chatRooms = chatQueryRepository.getMyChatRooms(memberId, pageable, keyword);

        return PageUt.of(chatRooms);
    }

    public ChatRoomDto getChatRoom(Long chatRoomId, long memberId) {
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

    private Member getMember(long memberId, ChatRoom chatRoom) {
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
}
