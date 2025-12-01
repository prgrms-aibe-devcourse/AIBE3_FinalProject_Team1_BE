package com.back.domain.post.dto.res;

import java.util.List;

public record GenPostDetailResBody(
        String title,
        String content,
        Long categoryId,
        Integer fee,
        Integer deposit,
        List<PostOption> options
) {
    public record PostOption(
        String name,
        Integer fee,
        Integer deposit
    ) {
    }
}
