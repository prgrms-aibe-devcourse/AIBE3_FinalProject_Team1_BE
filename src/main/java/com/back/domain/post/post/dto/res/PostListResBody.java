package com.back.domain.post.post.dto.res;

import com.back.domain.post.post.common.ReceiveMethod;
import com.back.domain.post.post.common.ReturnMethod;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record PostListResBody(
        Long postId,
        String title,
        String thumbnailImageUrl,
        Long categoryId,
        List<Long> regionIds,
        ReceiveMethod receiveMethod,
        ReturnMethod returnMethod,
        LocalDateTime createdAt,
        String authorNickname,
        Integer fee,
        Integer deposit,
        Boolean isFavorite,
        Boolean isBanned

        //TODO: 추후 추가 필드 있을 수 있음
        //TODO: 후기 평균 평점 + 갯수

) {
}
