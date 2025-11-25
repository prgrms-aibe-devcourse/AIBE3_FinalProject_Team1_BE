package com.back.domain.post.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PostAiContentReqBody(
        @NotBlank(message = "제목을 입력하세요.")
        String title,

        @NotNull(message = "카테고리를 선택하세요.")
        Long categoryId,

        @NotNull(message = "보증금을 입력하세요.")
        Integer deposit,

        @NotNull(message = "대여료를 입력하세요.")
        Integer fee,

        @Size(max = 5, message = "옵션은 최대 5개까지 등록할 수 있습니다.")
        List<PostOptionReqBody> options
) {
}
