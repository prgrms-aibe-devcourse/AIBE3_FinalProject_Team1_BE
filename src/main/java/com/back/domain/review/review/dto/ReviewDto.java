package com.back.domain.review.review.dto;

public record ReviewDto(
        Long id,
        int equipmentsScore,
        int kindnessScore,
        int responseTimeScore,
        String comment,
        ReviewAuthorDto author
) {
}
