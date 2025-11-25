package com.back.domain.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record MemberSendCodeReqBody(
        @Email
        @NotBlank
        String email
) {
}
