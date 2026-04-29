package com.gamelog.gamelog.controller.dto;

import java.time.Instant;

public record GameReviewResponse(
        Long id,
        Integer score,
        String review,
        String username,
        Instant createdAt
) {
}