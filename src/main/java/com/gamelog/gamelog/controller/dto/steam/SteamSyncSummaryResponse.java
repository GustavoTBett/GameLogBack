package com.gamelog.gamelog.controller.dto.steam;

import com.gamelog.gamelog.service.steam.SteamSyncService;

public record SteamSyncSummaryResponse(
        int totalReviews,
        int importedGames,
        int savedReviews
) {

    public static SteamSyncSummaryResponse from(SteamSyncService.ImportSummary summary) {
        return new SteamSyncSummaryResponse(
                summary.totalReviews(),
                summary.importedGames(),
                summary.savedReviews()
        );
    }
}
