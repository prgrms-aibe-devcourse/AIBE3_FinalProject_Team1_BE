package com.back.domain.post.dto.res;

import com.back.domain.post.entity.PostOption;

public record PostOptionResBody(
        String name,
        Integer deposit,
        Integer fee
) {
    public static PostOptionResBody of(PostOption option) {
        return new PostOptionResBody(
                option.getName(),
                option.getDeposit(),
                option.getFee()
        );
    }
}
