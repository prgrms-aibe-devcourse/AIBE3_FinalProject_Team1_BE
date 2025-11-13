package com.back.domain.reservation.dto;

import com.back.domain.member.member.entity.Member;

public record AuthorDto(
        Long authorId,
        String nickname,
        String profileImgUrl
) {
    public AuthorDto(Member member) {
        this (
                member.getId(),
                member.getName(),
                member.getProfileImgUrl()
        );
    }
}
