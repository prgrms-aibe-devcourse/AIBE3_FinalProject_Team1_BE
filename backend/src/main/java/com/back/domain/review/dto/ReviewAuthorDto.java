package com.back.domain.review.dto;

import com.back.domain.member.entity.Member;

public record ReviewAuthorDto(
        Long id,
        String nickname,
        String profileImgUrl
) {
    ReviewAuthorDto(Member member){
        this(
                member.getId(),
                member.getNickname(),
                member.getProfileImgUrl()
        );
    }
}
