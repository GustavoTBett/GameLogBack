package com.gamelog.gamelog.controller.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RatingRequest(
        @NotNull(message = "gameId is required") Long gameId,
        @NotNull(message = "rating is required")
        @Min(value = 1, message = "rating must be at least 1")
        @Max(value = 10, message = "rating must be at most 10")
        Integer score,
        String review
) {
}

