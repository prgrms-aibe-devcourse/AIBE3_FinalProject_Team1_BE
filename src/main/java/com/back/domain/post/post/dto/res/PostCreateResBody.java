package com.back.domain.post.post.dto.res;

import lombok.Builder;

@Builder
public record PostCreateResBody(
        String message,
        Long postId
) {}
