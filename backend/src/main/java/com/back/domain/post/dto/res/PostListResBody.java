package com.back.domain.post.dto.res;

import com.back.domain.post.common.ReceiveMethod;
import com.back.domain.post.common.ReturnMethod;
import com.back.domain.post.entity.Post;

import java.time.LocalDateTime;
import java.util.List;


public record PostListResBody(
        Long id,
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
    public static PostListResBody of(Post post, boolean isFavorite, String thumbnailImageUrl) {
        List<Long> regionIds = post.getPostRegions().stream()
                .map(r -> r.getRegion().getId())
                .toList();

        return new PostListResBody(
                post.getId(),
                post.getTitle(),
                thumbnailImageUrl,
                post.getCategory().getId(),
                regionIds,
                post.getReceiveMethod(),
                post.getReturnMethod(),
                post.getCreatedAt(),
                post.getAuthor().getNickname(),
                post.getFee(),
                post.getDeposit(),
                isFavorite,
                post.getIsBanned()

        );
    }
}

