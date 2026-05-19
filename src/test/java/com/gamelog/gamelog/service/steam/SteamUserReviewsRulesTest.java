package com.gamelog.gamelog.service.steam;

import com.gamelog.gamelog.model.Game;
import com.gamelog.gamelog.model.SteamAccount;
import com.gamelog.gamelog.model.SteamUserReview;
import com.gamelog.gamelog.repository.GameRepository;
import com.gamelog.gamelog.repository.SteamAccountRepository;
import com.gamelog.gamelog.repository.SteamUserReviewRepository;
import com.gamelog.gamelog.service.recommendation.RawgGameImportService;
import com.gamelog.gamelog.service.translation.TranslationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class SteamUserReviewsRulesTest {

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
    void importUserReviews_multipleReviews_importsAllAndSavesEach() {
        SteamAccount account = SteamAccount.builder().steamId("76561198000001000").build();

        when(steamService.getPlayerSummary("76561198000001000")).thenReturn(Map.of("communityvisibilitystate", 3));
        when(steamService.getUserReviews("76561198000001000")).thenReturn(List.of(
                review(1001L, "First Game", "Great", true, 1700000000L),
                review(1002L, "Second Game", "Awesome", false, 1700000100L)
        ));

        Game firstGame = Game.builder().name("First Game").slug("first-game").averageRating(0.0).defaultRating(0.0).build();
        when(gameRepository.findByName("First Game")).thenReturn(Optional.of(firstGame));

        Game importedSecondGame = Game.builder().name("Second Game").slug("second-game").averageRating(0.0).defaultRating(0.0).build();
        when(gameRepository.findByName("Second Game")).thenReturn(Optional.empty());
        when(rawgGameImportService.importGameByName("Second Game")).thenReturn(importedSecondGame);

        when(translationService.translate("Great", "pt")).thenReturn("Ótimo");
        when(translationService.translate("Awesome", "pt")).thenReturn("Incrível");

        var summary = syncService.importUserReviews(account, "76561198000001000");

        assertThat(summary.totalReviews()).isEqualTo(2);
        assertThat(summary.importedGames()).isEqualTo(1);
        assertThat(summary.savedReviews()).isEqualTo(2);
        verify(steamUserReviewRepository, times(2)).save(any(SteamUserReview.class));
    }

    @Test
    void importUserReviews_steamError_returnsControlledMessage() {
        SteamAccount account = SteamAccount.builder().steamId("76561198000001001").build();
        when(steamService.getPlayerSummary("76561198000001001")).thenReturn(Map.of("communityvisibilitystate", 3));
        when(steamService.getUserReviews("76561198000001001")).thenThrow(new RuntimeException("steam down"));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> syncService.importUserReviews(account, "76561198000001001"));

        assertThat(ex.getMessage()).isEqualTo("Não foi possível buscar as avaliações deste perfil Steam.");
        verify(steamUserReviewRepository, never()).save(any());
    }

    @Test
    void importUserReviews_incompleteResponse_returnsControlledMessage() {
        SteamAccount account = SteamAccount.builder().steamId("76561198000001002").build();
        when(steamService.getPlayerSummary("76561198000001002")).thenReturn(Map.of("communityvisibilitystate", 3));
        List<Map<String, Object>> incomplete = List.of(new HashMap<>(Map.of("review", "missing app id")));
        when(steamService.getUserReviews("76561198000001002")).thenReturn(incomplete);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> syncService.importUserReviews(account, "76561198000001002"));

        assertThat(ex.getMessage()).isEqualTo("Resposta incompleta da Steam para este perfil Steam.");
        verify(steamUserReviewRepository, never()).save(any());
        verify(rawgGameImportService, never()).importGameByName(anyString());
    }

    @Test
    void importUserReviews_sameReview_doesNotSaveAgain() {
        SteamAccount account = SteamAccount.builder().steamId("76561198000001003").build();
        Game sameGame = Game.builder().name("Same Game").slug("same-game").averageRating(0.0).defaultRating(0.0).build();
        SteamUserReview existing = SteamUserReview.builder()
                .steamAccount(account)
                .appId(3001L)
                .game(sameGame)
                .reviewText("Texto igual")
                .language("en")
                .recommended(true)
                .reviewedAt(Instant.ofEpochSecond(1700000200L))
                .active(true)
                .build();

        when(steamUserReviewRepository.findAllBySteamAccount(account)).thenReturn(List.of(existing));
        when(steamService.getPlayerSummary("76561198000001003")).thenReturn(Map.of("communityvisibilitystate", 3));
        when(steamService.getUserReviews("76561198000001003")).thenReturn(List.of(
                review(3001L, "Same Game", "Texto igual", true, 1700000200L)
        ));
        when(gameRepository.findByName("Same Game")).thenReturn(Optional.of(sameGame));

        var summary = syncService.importUserReviews(account, "76561198000001003");

        assertThat(summary.savedReviews()).isEqualTo(0);
        verify(steamUserReviewRepository, never()).save(any());
        assertThat(existing.getReviewText()).isEqualTo("Texto igual");
        assertThat(existing.getActive()).isTrue();
    }

    @Test
    void importUserReviews_changedReview_updatesExistingReview() {
        SteamAccount account = SteamAccount.builder().steamId("76561198000001004").build();
        Game sameGame = Game.builder().name("Update Game").slug("update-game").averageRating(0.0).defaultRating(0.0).build();
        SteamUserReview existing = SteamUserReview.builder()
                .steamAccount(account)
                .appId(4001L)
                .game(sameGame)
                .reviewText("Texto antigo")
                .language("en")
                .recommended(false)
                .reviewedAt(Instant.ofEpochSecond(1700000300L))
                .active(true)
                .build();

        when(steamUserReviewRepository.findAllBySteamAccount(account)).thenReturn(List.of(existing));
        when(steamService.getPlayerSummary("76561198000001004")).thenReturn(Map.of("communityvisibilitystate", 3));
        when(steamService.getUserReviews("76561198000001004")).thenReturn(List.of(
                review(4001L, "Update Game", "Texto novo", true, 1700000400L)
        ));
        when(gameRepository.findByName("Update Game")).thenReturn(Optional.of(sameGame));
        when(translationService.translate("Texto novo", "pt")).thenReturn("Texto novo traduzido");

        var summary = syncService.importUserReviews(account, "76561198000001004");

        assertThat(summary.savedReviews()).isEqualTo(1);
        verify(steamUserReviewRepository, times(1)).save(existing);
        assertThat(existing.getReviewText()).isEqualTo("Texto novo traduzido");
        assertThat(existing.getRecommended()).isTrue();
        assertThat(existing.getReviewedAt()).isEqualTo(Instant.ofEpochSecond(1700000400L));
    }

    @Test
    void importUserReviews_removedReview_marksExistingInactive() {
        SteamAccount account = SteamAccount.builder().steamId("76561198000001005").build();
        Game sameGame = Game.builder().name("Removed Game").slug("removed-game").averageRating(0.0).defaultRating(0.0).build();
        SteamUserReview existing = SteamUserReview.builder()
                .steamAccount(account)
                .appId(5001L)
                .game(sameGame)
                .reviewText("Texto que sumiu")
                .language("en")
                .recommended(true)
                .reviewedAt(Instant.ofEpochSecond(1700000500L))
                .active(true)
                .build();

        when(steamUserReviewRepository.findAllBySteamAccount(account)).thenReturn(List.of(existing));
        when(steamService.getPlayerSummary("76561198000001005")).thenReturn(Map.of("communityvisibilitystate", 3));
        when(steamService.getUserReviews("76561198000001005")).thenReturn(List.of());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> syncService.importUserReviews(account, "76561198000001005"));

        assertThat(ex.getMessage()).isEqualTo("Nenhuma avaliação pública encontrada para este perfil Steam.");
        assertThat(existing.getActive()).isFalse();
        verify(steamUserReviewRepository, times(1)).save(existing);
    }

    private static Map<String, Object> review(long appId, String appName, String text, boolean votedUp, long createdAt) {
        Map<String, Object> review = new HashMap<>();
        review.put("appid", appId);
        review.put("app_name", appName);
        review.put("review", text);
                review.put("language", "en");
        review.put("voted_up", votedUp);
        review.put("timestamp_created", createdAt);
        return review;
    }
}
