package com.back.domain.post.dto.res;

import com.back.domain.post.entity.PostOption;

public record PostOptionResBody(
        Long id,
        String name,
        Integer deposit,
        Integer fee
) {
    public static PostOptionResBody of(PostOption option) {
        return new PostOptionResBody(
                option.getId(),
                option.getName(),
                option.getDeposit(),
                option.getFee()
        );
    }
}
