package com.back.domain.post.post.dto;

import com.back.domain.post.post.common.ReceiveMethod;
import com.back.domain.post.post.common.ReturnMethod;
import lombok.Builder;

import java.util.List;

@Builder
public record PostCreateReqBody(
        String title,
        String content,
        ReceiveMethod receiveMethod,
        ReturnMethod returnMethod,
        String returnAddress1,
        String returnAddress2,
        List<Long> regionIds,
        Long categoryId,
        Integer deposit,
        Integer fee,
        List<PostOptionReqBody> options,
        List<PostImageReqBody> images
) {
}
