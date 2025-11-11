package com.back.domain.member.member.dto;

import com.back.domain.member.member.entity.Member;

import java.time.LocalDateTime;

public record MemberDto(
        Long id,
        String email,
        String name,
        String nickname,
        String phoneNumber,
        String address1,
        String address2,
        String profileImgUrl,
        LocalDateTime createdAt,
        String role,
        boolean isBanned
) {
    public MemberDto(Member member) {
        this(
                member.getId(),
                member.getEmail(),
                member.getName(),
                member.getNickname(),
                member.getPhoneNumber(),
                member.getAddress1(),
                member.getAddress2(),
                member.getProfileImgUrl(),
                member.getCreatedAt(),
                member.getRole().name(),
                member.isBanned()
        );
    }
}
