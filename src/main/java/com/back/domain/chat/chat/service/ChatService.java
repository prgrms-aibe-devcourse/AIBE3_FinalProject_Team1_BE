package com.back.domain.chat.chat.service;

import com.back.domain.chat.chat.dto.ChatRoomDto;
import com.back.domain.chat.chat.dto.CreateChatRoomResBody;
import com.back.domain.chat.chat.entity.ChatRoom;
import com.back.domain.chat.chat.repository.ChatRoomRepository;
import com.back.domain.member.entity.Member;
import com.back.domain.member.service.MemberService;
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
    // TODO : 주입 계층 통일
    private final MemberService memberService;
    private final PostRepository postRepository;
    private final ChatRoomRepository chatRoomRepository;

    @Transactional
    public CreateChatRoomResBody createOrGetChatRoom(Long postId, Long memberId) {

        Post post = postRepository.findById(postId).orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "존재하지 않는 게시글입니다."));
        Member host = post.getAuthor();

        if (host.getId().equals(memberId)) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "본인과 채팅방을 만들 수 없습니다.");
        }

        Optional<Long> existingRoom = chatRoomRepository.findIdByPostAndMembers(postId, host.getId(), memberId);
        if (existingRoom.isPresent()) {
            Long roomId = existingRoom.get();
            return new CreateChatRoomResBody("이미 존재하는 채팅방입니다.", roomId);
        }

        ChatRoom chatRoom = ChatRoom.builder()
                .post(post)
                .build();

        Member guest = memberService.getById(memberId);
        chatRoom.addMember(host);
        chatRoom.addMember(guest);

        chatRoomRepository.save(chatRoom);

        return new CreateChatRoomResBody("채팅방이 생성되었습니다.", chatRoom.getId());
    }

    public PagePayload<ChatRoomDto> getMyChatRooms(Long memberId, Pageable pageable, String keyword) {
        Page<ChatRoomDto> chatRooms = chatRoomRepository.findByMemberId(memberId, pageable, keyword);

        return PageUt.of(chatRooms);
    }
}