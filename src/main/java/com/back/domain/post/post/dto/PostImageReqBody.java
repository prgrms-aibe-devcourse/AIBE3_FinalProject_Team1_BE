package com.back.domain.post.post.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record PostImageReqBody(
        @NotNull(message = "대표 이미지 여부를 입력하세요")
        Boolean isPrimary
) {
}
