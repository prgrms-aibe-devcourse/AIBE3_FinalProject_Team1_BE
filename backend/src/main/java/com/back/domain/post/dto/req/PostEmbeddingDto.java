package com.back.domain.post.dto.req;

import com.back.domain.post.entity.Post;

import java.util.List;

public record PostEmbeddingDto(
        Long id,
        String title,
        String content,
        String categoryName,
        Integer fee,
        Integer deposit,
        String receiveMethod,
        String returnMethod,
        List<Long> regionIds,
        Long embeddingVersion
) {
    public static PostEmbeddingDto from(Post post) {
        return new PostEmbeddingDto(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getCategory().getName(),
                post.getFee(),
                post.getDeposit(),
                post.getReceiveMethod().toString(),
                post.getReturnMethod().toString(),
                post.getPostRegions().stream()
                        .map(pr -> pr.getRegion().getId())
                        .toList(),
                post.getEmbeddingVersion()
        );
    }
}
