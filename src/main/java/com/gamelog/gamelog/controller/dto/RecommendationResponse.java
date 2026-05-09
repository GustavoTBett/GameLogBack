package com.gamelog.gamelog.controller.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record RecommendationResponse(
        String gameName,
        String gameSlug,
        String reason,
        Long gameId,
        Double averageRating,
        Double defaultRating,
        LocalDate releaseDate,
        Boolean isNewlyImported,
        String coverUrl,
        OffsetDateTime generatedAt
) {}
