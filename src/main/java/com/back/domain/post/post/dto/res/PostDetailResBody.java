package com.back.domain.post.post.dto.res;

import com.back.domain.member.member.dto.AuthorDto;
import com.back.domain.post.post.common.ReceiveMethod;
import com.back.domain.post.post.common.ReturnMethod;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record PostDetailResBody(
        Long postId,
        String title,
        String content,
        Long categoryId,
        List<Long> regionIds,
        ReceiveMethod receiveMethod,
        ReturnMethod returnMethod,
        String returnAddress1,
        String returnAddress2,
        Integer deposit,
        Integer fee,
        List<PostOptionResBody> options,
        List<PostImageResBody> images,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt,
        AuthorDto author,
        Boolean isFavorite,
        Boolean isBanned
) {
}
