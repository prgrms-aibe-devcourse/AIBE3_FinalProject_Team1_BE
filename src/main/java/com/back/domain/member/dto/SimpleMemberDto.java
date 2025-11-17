package com.back.domain.member.dto;

import com.back.domain.member.entity.Member;

import java.time.LocalDateTime;

public record SimpleMemberDto(
        Long id,
        String nickname,
        String profileImgUrl,
        Boolean isBanned,
        LocalDateTime createdAt
) {
    public SimpleMemberDto(Member member){
        this(
                member.getId(),
                member.getNickname(),
                member.getProfileImgUrl(),
                member.isBanned(),
                member.getCreatedAt()
        );
    }
}
