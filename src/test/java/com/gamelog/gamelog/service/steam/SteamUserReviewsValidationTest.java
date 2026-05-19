package com.gamelog.gamelog.service.steam;

import com.gamelog.gamelog.model.SteamAccount;
import com.gamelog.gamelog.repository.GameRepository;
import com.gamelog.gamelog.repository.SteamAccountRepository;
import com.gamelog.gamelog.repository.SteamUserReviewRepository;
import com.gamelog.gamelog.service.recommendation.RawgGameImportService;
import com.gamelog.gamelog.service.translation.TranslationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class SteamUserReviewsValidationTest {

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
    void importUserReviews_nullProfile_returnsValidationError() {
        SteamAccount account = SteamAccount.builder().steamId("123").build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> syncService.importUserReviews(account, null));

        assertThat(ex.getMessage()).isEqualTo("Perfil Steam é obrigatório.");
        verifyNoInteractions(steamService, rawgGameImportService, steamUserReviewRepository);
    }

    @Test
    void importUserReviews_blankProfile_returnsValidationError() {
        SteamAccount account = SteamAccount.builder().steamId("123").build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> syncService.importUserReviews(account, "   "));

        assertThat(ex.getMessage()).isEqualTo("Perfil Steam é obrigatório.");
        verifyNoInteractions(steamService, rawgGameImportService, steamUserReviewRepository);
    }

    @Test
    void importUserReviews_invalidProfileFormat_returnsValidationError() {
        SteamAccount account = SteamAccount.builder().steamId("123").build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> syncService.importUserReviews(account, "https://example.com/not-steam"));

        assertThat(ex.getMessage()).isEqualTo("Perfil Steam inválido.");
        verifyNoInteractions(steamService, rawgGameImportService, steamUserReviewRepository);
    }

    @Test
    void importUserReviews_validProfileUrl_allowsContinuity() {
        SteamAccount account = SteamAccount.builder().steamId("link").build();
        when(steamService.resolveVanityUrl("myvanity")).thenReturn("76561198000000000");
        when(steamService.getPlayerSummary("76561198000000000")).thenReturn(Map.of("communityvisibilitystate", 3));
        when(steamService.getUserReviews("76561198000000000")).thenReturn(List.of());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> syncService.importUserReviews(account, "https://steamcommunity.com/id/myvanity"));

        assertThat(ex.getMessage()).isEqualTo("Nenhuma avaliação pública encontrada para este perfil Steam.");
        verify(steamService, times(1)).resolveVanityUrl("myvanity");
        verify(rawgGameImportService, never()).importGameByName(anyString());
        verify(steamUserReviewRepository, never()).save(any());
    }

    @Test
    void importUserReviews_nonExistentProfile_returnsNotFoundMessage() {
        SteamAccount account = SteamAccount.builder().steamId("link").build();
        when(steamService.resolveVanityUrl("missingprofile")).thenReturn(null);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> syncService.importUserReviews(account, "missingprofile"));

        assertThat(ex.getMessage()).isEqualTo("Perfil Steam não encontrado.");
        verify(steamService, times(1)).resolveVanityUrl("missingprofile");
        verifyNoInteractions(rawgGameImportService, steamUserReviewRepository);
    }

    @Test
    void importUserReviews_privateProfile_returnsControlledNoReviewsMessage() {
        SteamAccount account = SteamAccount.builder().steamId("76561198000000003").build();
        when(steamService.getPlayerSummary("76561198000000003")).thenReturn(Map.of("communityvisibilitystate", 1));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> syncService.importUserReviews(account, "76561198000000003"));

        assertThat(ex.getMessage()).isEqualTo("Nenhuma avaliação pública encontrada para este perfil Steam.");
        verify(steamService, never()).getUserReviews(anyString());
        verifyNoInteractions(rawgGameImportService, steamUserReviewRepository);
    }
}
