package com.gamelog.gamelog.controller.dto.steam;

import com.gamelog.gamelog.model.Game;
import com.gamelog.gamelog.model.SteamUserReview;

import java.time.Instant;

public record SteamUserReviewResponse(
        Long appId,
        String gameName,
        String gameSlug,
        String coverUrl,
        String reviewText,
        String language,
        Boolean recommended,
        Instant reviewedAt,
        Boolean active,
        Instant importedAt
) {

    public static SteamUserReviewResponse from(SteamUserReview review) {
        Game game = review.getGame();

        return new SteamUserReviewResponse(
                review.getAppId(),
                game != null ? game.getName() : null,
                game != null ? game.getSlug() : null,
                game != null ? game.getCover_url() : null,
                review.getReviewText(),
                review.getLanguage(),
                review.getRecommended(),
                review.getReviewedAt(),
                review.getActive(),
                review.getImportedAt()
        );
    }
}
