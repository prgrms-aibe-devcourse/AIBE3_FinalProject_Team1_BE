package com.back.domain.review.dto;

public record ReviewSummaryDto(
        float equipmentScore,
        float kindnessScore,
        float responseTimeScore,
        float avgScore,
        long count
) {
    public static ReviewSummaryDto empty() {
        return new ReviewSummaryDto(0f, 0f, 0f, 0f, 0);
    }
}
