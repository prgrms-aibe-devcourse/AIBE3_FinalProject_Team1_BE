package com.back.domain.post.post.dto;

public record PostOptionReqBody(
        String name,
        Integer deposit,
        Integer fee
) {
}
