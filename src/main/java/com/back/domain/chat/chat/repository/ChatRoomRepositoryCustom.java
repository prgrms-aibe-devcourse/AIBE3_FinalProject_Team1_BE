package com.back.domain.chat.chat.repository;

import com.back.domain.chat.chat.dto.ChatRoomDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ChatRoomRepositoryCustom {
    Optional<Long> findIdByPostAndMembers(Long postId, Long hostId, Long guestId);
    Page<ChatRoomDto> findByMemberId(Long memberId, Pageable pageable, String keyword);
}
