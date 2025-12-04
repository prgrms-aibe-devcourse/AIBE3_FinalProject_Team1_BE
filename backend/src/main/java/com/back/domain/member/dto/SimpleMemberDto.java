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
    public SimpleMemberDto(Member member, String presignedUrl){
        this(
                member.getId(),
                member.getNickname(),
                presignedUrl,
                member.isBanned(),
                member.getCreatedAt()
        );
    }
}
