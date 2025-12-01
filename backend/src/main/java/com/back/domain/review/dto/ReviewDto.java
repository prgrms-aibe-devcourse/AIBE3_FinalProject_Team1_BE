package com.back.domain.review.dto;

import com.back.domain.member.entity.Member;
import com.back.domain.review.entity.Review;

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
    public ReviewDto(Review review, Member member) {
        this(
                review.getId(),
                review.getEquipmentScore(),
                review.getKindnessScore(),
                review.getResponseTimeScore(),
                review.getComment(),
                review.getCreatedAt(),
                new ReviewAuthorDto(member)
        );
    }
}
