package com.gamelog.gamelog.controller.dto;

import com.gamelog.gamelog.model.enums.RatingVoteType;
import jakarta.validation.constraints.NotNull;

public record RatingVoteRequest(
        @NotNull(message = "voteType is required")
        RatingVoteType voteType
) {
}