package com.back.domain.post.post.dto;

import com.back.domain.post.post.common.ReceiveMethod;
import com.back.domain.post.post.common.ReturnMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.List;

@Builder
public record PostCreateReqBody(

        @NotBlank(message = "제목을 입력하세요.")
        String title,
        @NotBlank(message = "내용을 입력하세요.")
        String content,
        @NotNull(message = "수령 방식을 선택하세요.")
        ReceiveMethod receiveMethod,
        @NotNull(message = "반납 방식을 선택하세요.")
        ReturnMethod returnMethod,
        String returnAddress1,
        String returnAddress2,
        @NotEmpty(message = "지역을 하나 이상 선택하세요.")
        List<Long> regionIds,
        @NotNull(message = "카테고리를 선택하세요.")
        Long categoryId,
        @NotNull(message = "보증금을 입력하세요.")
        Integer deposit,
        @NotNull(message = "대여료을 입력하세요.")
        Integer fee,
        @Size(max = 5, message = "옵션은 최대 5개까지 등록할 수 있습니다.")
        List<PostOptionReqBody> options,
        @NotEmpty(message = "이미지를 하나 이상 등록하세요.")
        @Size(max = 10, message = "이미지는 최대 10개까지 등록할 수 있습니다.")
        List<PostImageReqBody> images
) {
}
