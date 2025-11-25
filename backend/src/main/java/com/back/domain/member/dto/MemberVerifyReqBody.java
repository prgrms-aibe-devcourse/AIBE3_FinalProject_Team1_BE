package com.back.domain.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record MemberVerifyReqBody(
        @Email
        @NotBlank
        String email,

        @NotBlank
        String code
) {
}
