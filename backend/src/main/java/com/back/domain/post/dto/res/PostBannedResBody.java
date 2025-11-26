package com.back.domain.post.dto.res;

import com.back.domain.post.entity.Post;

import java.time.LocalDateTime;

public record PostBannedResBody(
        Long id,
        String title,
        String content,
        Long authorId,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt,
        Boolean isBanned
) {
    public static PostBannedResBody of(Post post) {
        return new PostBannedResBody(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getAuthor().getId(),
                post.getCreatedAt(),
                post.getModifiedAt(),
                post.getIsBanned()
        );
    }
}
