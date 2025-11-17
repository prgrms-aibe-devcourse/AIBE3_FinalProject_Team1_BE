package com.back.domain.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReviewWriteReqBody(
        @NotNull
        @Min(1)
        @Max(5)
        int equipmentScore,
        @NotNull
        @Min(1)
        @Max(5)
        int kindnessScore,
        @NotNull
        @Min(1)
        @Max(5)
        int responseTimeScore,
        @NotBlank
        String comment
) {
}
