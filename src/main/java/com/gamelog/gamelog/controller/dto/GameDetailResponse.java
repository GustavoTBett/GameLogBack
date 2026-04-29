package com.gamelog.gamelog.controller.dto;

import com.gamelog.gamelog.model.EnumUser.GamePlatform;

import java.time.LocalDate;
import java.util.List;

public record GameDetailResponse(
        Long id,
        String name,
        String slug,
        String description,
        String descriptionPtBr,
        String coverUrl,
        Double averageRating,
        LocalDate releaseDate,
        String developer,
        Long totalReviews,
        List<String> genres,
        List<GamePlatform> platforms,
        List<GameReviewResponse> reviews
) {
}