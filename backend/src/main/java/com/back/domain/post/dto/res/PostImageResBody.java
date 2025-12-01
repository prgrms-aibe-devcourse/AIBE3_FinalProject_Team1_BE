package com.back.domain.post.dto.res;

import com.back.domain.post.entity.PostImage;

public record PostImageResBody(
        Long id,
        String file,
        Boolean isPrimary
) {
    public static PostImageResBody of(PostImage image, String presignedUrl) {
        return new PostImageResBody(
                image.getId(),
                presignedUrl,
                image.getIsPrimary()
        );
    }
}
