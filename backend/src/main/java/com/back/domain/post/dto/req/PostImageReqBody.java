package com.back.domain.post.dto.req;


import jakarta.validation.constraints.NotNull;

public record PostImageReqBody(
        Long id,
        @NotNull(message = "대표 이미지 여부를 입력하세요")
        Boolean isPrimary
) {
}
