package com.back.domain.post.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record PostOptionReqBody(
        @NotBlank(message = "옵션 이름을 입력하세요.")
        String name,
        @NotNull(message = "추가 보증금을 입력하세요.")
        Integer deposit,
        @NotNull(message = "추가 대여료를 입력하세요.")
        Integer fee
) {
}
