package com.back.domain.review.dto;

import java.time.LocalDateTime;

public record ReviewDto(
        Long id,
        int equipmentScore,
        int kindnessScore,
        int responseTimeScore,
        String comment,
        LocalDateTime createdAt,
        ReviewAuthorDto author
) {
}
