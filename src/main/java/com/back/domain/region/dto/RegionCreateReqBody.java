package com.back.domain.region.dto;

import jakarta.validation.constraints.NotBlank;

public record RegionCreateReqBody(
        Long parentId,

        @NotBlank
        String name
) {
}
