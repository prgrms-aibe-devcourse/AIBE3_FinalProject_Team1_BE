package com.back.domain.category.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoryCreateReqBody(
        Long parentId,

        @NotBlank
        String name
) {
}
