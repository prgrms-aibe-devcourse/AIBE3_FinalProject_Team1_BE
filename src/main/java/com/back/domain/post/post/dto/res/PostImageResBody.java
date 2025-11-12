package com.back.domain.post.post.dto.res;

import lombok.Builder;

@Builder
public record PostImageResBody(
        String file,
        Boolean isPrimary
) {
}
