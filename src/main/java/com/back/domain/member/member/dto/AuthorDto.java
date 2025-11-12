package com.back.domain.member.member.dto;

import com.back.domain.member.member.entity.Member;
import lombok.Builder;

@Builder
public record AuthorDto(
        Long id,
        String nickname,
        String profileImgUrl
) {
    public static AuthorDto from(Member member) {
        return AuthorDto.builder()
                .id(member.getId())
                .nickname(member.getNickname())
                .profileImgUrl(member.getProfileImgUrl())
                .build();
    }
}
