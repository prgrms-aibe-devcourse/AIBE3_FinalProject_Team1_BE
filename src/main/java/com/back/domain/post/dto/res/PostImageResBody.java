package com.back.domain.post.dto.res;

import com.back.domain.post.entity.PostImage;

public record PostImageResBody(
        String file,
        Boolean isPrimary
) {
    public static PostImageResBody of(PostImage image) {
        return new PostImageResBody(
                image.getImageUrl(),
                image.getIsPrimary()
        );
    }
}
