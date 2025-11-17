package com.back.domain.post.dto.res;

import com.back.domain.post.entity.Post;

import java.time.LocalDateTime;

public record PostCreateResBody(
        Long id,
        String title,
        String content,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt
) {
    public static PostCreateResBody of(Post post) {
        return new PostCreateResBody(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getCreatedAt(),
                post.getModifiedAt()
        );
    }
}
