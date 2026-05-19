package com.gamelog.gamelog.service.steam;

import com.gamelog.gamelog.model.*;
import com.gamelog.gamelog.repository.SteamUserReviewRepository;
import com.gamelog.gamelog.repository.SteamAccountRepository;
import com.gamelog.gamelog.repository.GameRepository;
import com.gamelog.gamelog.service.recommendation.RawgGameImportService;
import com.gamelog.gamelog.service.translation.TranslationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class SteamUserReviewsImportFlowTest {

    private SteamService steamService;
    private SteamAccountRepository steamAccountRepository;
    private RawgGameImportService rawgGameImportService;
    private GameRepository gameRepository;
    private SteamUserReviewRepository steamUserReviewRepository;
    private TranslationService translationService;
    private SteamSyncService syncService;

    @BeforeEach
    void setup() {
        steamService = mock(SteamService.class);
        steamAccountRepository = mock(SteamAccountRepository.class);
        rawgGameImportService = mock(RawgGameImportService.class);
        gameRepository = mock(GameRepository.class);
        steamUserReviewRepository = mock(SteamUserReviewRepository.class);
        translationService = mock(TranslationService.class);

        when(steamUserReviewRepository.findAllBySteamAccount(any())).thenReturn(List.of());

        syncService = new SteamSyncService(
                steamService,
                steamAccountRepository,
                rawgGameImportService,
                gameRepository,
                steamUserReviewRepository,
                translationService
        );
    }

    @Test
    void importUserReviews_success_importsGame_translates_and_saves() {
        SteamAccount account = SteamAccount.builder().steamId("76561198000000000").build();

        when(steamService.getPlayerSummary("76561198000000000")).thenReturn(Map.of("communityvisibilitystate", 3));

        Map<String, Object> review = Map.of("appid", 111, "app_name", "New Game", "review", "I love this game");
        when(steamService.getUserReviews("76561198000000000")).thenReturn(List.of(review));

        when(gameRepository.findByName("New Game")).thenReturn(Optional.empty());
        Game imported = Game.builder().name("New Game").slug("new-game").averageRating(0.0).defaultRating(0.0).build();
        when(rawgGameImportService.importGameByName("New Game")).thenReturn(imported);

        when(translationService.translate("I love this game", "pt")).thenReturn("Eu amo este jogo");

        when(steamUserReviewRepository.findBySteamAccountAndAppId(account, 111L)).thenReturn(Optional.empty());

        var summary = syncService.importUserReviews(account, "76561198000000000");

        assertThat(summary.totalReviews()).isEqualTo(1);
        assertThat(summary.importedGames()).isEqualTo(1);
        assertThat(summary.savedReviews()).isEqualTo(1);

        verify(steamUserReviewRepository, times(1)).save(any(SteamUserReview.class));
    }

    @Test
    void importUserReviews_profilePrivate_throws() {
        SteamAccount account = SteamAccount.builder().steamId("76561198000000001").build();
        when(steamService.getPlayerSummary("76561198000000001")).thenReturn(Map.of("communityvisibilitystate", 1));

        assertThrows(IllegalStateException.class, () -> syncService.importUserReviews(account, "76561198000000001"));
    }

    @Test
    void importUserReviews_gameExists_reusesAndSavesReview() {
        SteamAccount account = SteamAccount.builder().steamId("76561198000000002").build();
        when(steamService.getPlayerSummary("76561198000000002")).thenReturn(Map.of("communityvisibilitystate", 3));

        Map<String, Object> review = Map.of("appid", 222, "app_name", "Exists Game", "review", "Nice");
        when(steamService.getUserReviews("76561198000000002")).thenReturn(List.of(review));

        Game existing = Game.builder().name("Exists Game").slug("exists-game").averageRating(0.0).defaultRating(0.0).build();
        when(gameRepository.findByName("Exists Game")).thenReturn(Optional.of(existing));

        when(steamUserReviewRepository.findBySteamAccountAndAppId(account, 222L)).thenReturn(Optional.empty());

        var summary = syncService.importUserReviews(account, "76561198000000002");

        assertThat(summary.totalReviews()).isEqualTo(1);
        assertThat(summary.importedGames()).isEqualTo(0);
        assertThat(summary.savedReviews()).isEqualTo(1);

        verify(rawgGameImportService, never()).importGameByName(anyString());
        verify(steamUserReviewRepository, times(1)).save(any(SteamUserReview.class));
    }
}
