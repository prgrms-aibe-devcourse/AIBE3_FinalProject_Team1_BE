package com.back.domain.review.dto;

import com.back.domain.review.entity.Review;

import java.time.LocalDateTime;

public record ReviewBannedResBody(
        Long id,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt,
        Long authorId,
        Boolean isBanned
) {
    public static ReviewBannedResBody of(Review review) {
        return new ReviewBannedResBody(
                review.getId(),
                review.getCreatedAt(),
                review.getModifiedAt(),
                review.getReservation().getAuthor().getId(),
                review.isBanned()
        );
    }
}
