package com.back.domain.post.dto.res;

import com.back.domain.post.common.ReceiveMethod;
import com.back.domain.post.common.ReturnMethod;
import com.back.domain.post.entity.Post;

import java.time.LocalDateTime;
import java.util.List;

public record PostDetailResBody(
        Long id,
        String title,
        String content,
        Long categoryId,
        List<Long> regionIds,
        String returnAddress1,
        String returnAddress2,
        ReceiveMethod receiveMethod,
        ReturnMethod returnMethod,
        Integer deposit,
        Integer fee,
        List<PostOptionResBody> options,
        List<PostImageResBody> images,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt,
        PostAuthorDto author,
        Boolean isFavorite,
        Boolean isBanned
) {
    public static PostDetailResBody of(Post post, boolean isFavorite, List<PostImageResBody> images) {

        List<Long> regionIds = post.getPostRegions().stream()
                .map(r -> r.getRegion().getId())
                .toList();

        List<PostOptionResBody> options = post.getOptions().stream()
                .map(PostOptionResBody::of)
                .toList();

        return new PostDetailResBody(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getCategory().getId(),
                regionIds,
                post.getReturnAddress1(),
                post.getReturnAddress2(),
                post.getReceiveMethod(),
                post.getReturnMethod(),
                post.getDeposit(),
                post.getFee(),
                options,
                images,
                post.getCreatedAt(),
                post.getModifiedAt(),
                PostAuthorDto.from(post.getAuthor()),
                isFavorite,
                post.getIsBanned()
        );
    }
}
