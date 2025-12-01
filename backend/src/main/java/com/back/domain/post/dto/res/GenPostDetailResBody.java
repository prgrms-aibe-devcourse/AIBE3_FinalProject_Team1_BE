package com.back.domain.post.dto.res;

public record GenPostDetailResBody(
        String title,
        String content,
        Long parentCategoryId,
        Long childCategoryId,
        Integer fee,
        Integer deposit
) {
}
