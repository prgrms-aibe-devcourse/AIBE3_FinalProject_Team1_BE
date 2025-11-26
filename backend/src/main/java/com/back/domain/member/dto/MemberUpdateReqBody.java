package com.back.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MemberUpdateReqBody(
        @NotBlank
        String address1,
        @NotBlank
        String address2,
        @NotBlank
        String name,
        @NotBlank
        String phoneNumber,
        @NotNull
        Boolean removeProfileImage
) {
}
