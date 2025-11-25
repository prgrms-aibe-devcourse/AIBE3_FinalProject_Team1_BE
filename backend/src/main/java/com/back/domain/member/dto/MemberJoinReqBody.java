package com.back.domain.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemberJoinReqBody(
        @NotBlank
        @Email
        String email,
        @NotBlank
        @Size(min = 8)
        String password,
        @NotBlank
        String nickname
) {
}
