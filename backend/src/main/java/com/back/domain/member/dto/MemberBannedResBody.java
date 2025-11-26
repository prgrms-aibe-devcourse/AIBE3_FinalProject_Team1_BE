package com.back.domain.member.dto;

import com.back.domain.member.entity.Member;

import java.time.LocalDateTime;

public record MemberBannedResBody(
        Long id,
        String email,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt,
        Boolean isBanned
) {
    public static MemberBannedResBody of(Member member) {
        return new MemberBannedResBody(
                member.getId(),
                member.getEmail(),
                member.getCreatedAt(),
                member.getModifiedAt(),
                member.isBanned()
        );
    }
}
