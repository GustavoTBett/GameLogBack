package com.gamelog.gamelog.service.game;

import com.gamelog.gamelog.controller.dto.GameDetailResponse;
import com.gamelog.gamelog.controller.dto.GameReviewResponse;
import com.gamelog.gamelog.model.Game;
import com.gamelog.gamelog.model.Rating;
import com.gamelog.gamelog.model.SteamAccount;
import com.gamelog.gamelog.model.SteamUserReview;
import com.gamelog.gamelog.model.User;
import com.gamelog.gamelog.repository.GameGenreRepository;
import com.gamelog.gamelog.repository.GamePlatformMappingRepository;
import com.gamelog.gamelog.repository.GameRepository;
import com.gamelog.gamelog.repository.RatingRepository;
import com.gamelog.gamelog.repository.SteamUserReviewRepository;
import com.gamelog.gamelog.service.image.GameImageResolver;
import com.gamelog.gamelog.service.image.RawgImageBackfillService;
import com.gamelog.gamelog.service.ratingvote.RatingVoteService;
import com.gamelog.gamelog.service.ratingvote.RatingVoteStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GameServiceImplTest {

    private GameRepository gameRepository;
    private GamePlatformMappingRepository gamePlatformRepository;
    private GameGenreRepository gameGenreRepository;
    private RatingRepository ratingRepository;
    private SteamUserReviewRepository steamUserReviewRepository;
    private RatingVoteService ratingVoteService;
    private GameImageResolver gameImageResolver;
    private RawgImageBackfillService rawgImageBackfillService;
    private GameServiceImpl gameService;

    @BeforeEach
    void setUp() {
        gameRepository = mock(GameRepository.class);
        gamePlatformRepository = mock(GamePlatformMappingRepository.class);
        gameGenreRepository = mock(GameGenreRepository.class);
        ratingRepository = mock(RatingRepository.class);
        steamUserReviewRepository = mock(SteamUserReviewRepository.class);
        ratingVoteService = mock(RatingVoteService.class);
        gameImageResolver = mock(GameImageResolver.class);
        rawgImageBackfillService = mock(RawgImageBackfillService.class);

        gameService = new GameServiceImpl(
                gameRepository,
                gamePlatformRepository,
                gameGenreRepository,
                ratingRepository,
                steamUserReviewRepository,
                ratingVoteService,
                gameImageResolver,
                rawgImageBackfillService
        );
    }

    @Test
    void getSummaryBySlugSeparatesAppRatingsFromSteamReviews() {
        Game game = mock(Game.class);
        User user = mock(User.class);
        Rating rating = mock(Rating.class);
        SteamAccount steamAccount = mock(SteamAccount.class);
        SteamUserReview steamReview = mock(SteamUserReview.class);
        Instant reviewedAt = Instant.parse("2026-01-10T12:00:00Z");

        when(game.getId()).thenReturn(10L);
        when(game.getName()).thenReturn("Portal");
        when(game.getSlug()).thenReturn("portal");
        when(game.getDescription()).thenReturn("Puzzle");
        when(game.getDescriptionPtBr()).thenReturn("Quebra-cabeca");
        when(game.getAverageRating()).thenReturn(9.0);
        when(game.getDefaultRating()).thenReturn(8.5);
        when(game.getReleaseDate()).thenReturn(LocalDate.of(2007, 10, 10));
        when(game.getDeveloper()).thenReturn("Valve");

        when(user.getId()).thenReturn(100L);
        when(user.getUsername()).thenReturn("gustavo");

        when(rating.getId()).thenReturn(1L);
        when(rating.getScore()).thenReturn(9);
        when(rating.getReview()).thenReturn("Excelente");
        when(rating.getUser()).thenReturn(user);
        when(rating.getCreatedAt()).thenReturn(reviewedAt);
        when(rating.getUpdatedAt()).thenReturn(reviewedAt);

        when(steamAccount.getUser()).thenReturn(user);
        when(steamReview.getId()).thenReturn(7L);
        when(steamReview.getGame()).thenReturn(game);
        when(steamReview.getSteamAccount()).thenReturn(steamAccount);
        when(steamReview.getReviewText()).thenReturn("Great on Steam");
        when(steamReview.getRecommended()).thenReturn(true);
        when(steamReview.getReviewedAt()).thenReturn(reviewedAt.plusSeconds(60));
        when(steamReview.getImportedAt()).thenReturn(reviewedAt.plusSeconds(120));

        when(gameRepository.findBySlug("portal")).thenReturn(Optional.of(game));
        when(gameImageResolver.resolveAndPersistCoverUrl(game)).thenReturn("cover");
        when(ratingRepository.countRatingsByGameIds(anyCollection())).thenReturn(List.of(new Object[]{10L, 1L}));
        when(steamUserReviewRepository.findAllByGameIdInAndActiveTrue(anyCollection())).thenReturn(List.of(steamReview));
        when(gameGenreRepository.findAllByGameIdsWithGenre(anyCollection())).thenReturn(List.of());
        when(gamePlatformRepository.findAllByGameIdIn(anyCollection())).thenReturn(List.of());
        when(ratingRepository.findAllByGameIdOrderByCreatedAtDescIdDesc(10L)).thenReturn(List.of(rating));
        when(steamUserReviewRepository.findAllByGameIdAndActiveTrue(10L)).thenReturn(List.of(steamReview));
        when(ratingVoteService.getVoteStatsByRatingIds(anyCollection())).thenReturn(Map.of(1L, new RatingVoteStats(2L, 0L)));
        when(ratingVoteService.getUserVotesByRatingIds(anyCollection(), eq(100L))).thenReturn(Map.of());

        GameDetailResponse detail = gameService.getSummaryBySlug("portal", 100L).orElseThrow();

        assertThat(detail.totalReviews()).isEqualTo(2L);
        assertThat(detail.appReviewCount()).isEqualTo(1L);
        assertThat(detail.steamReviewCount()).isEqualTo(1L);

        GameReviewResponse appReview = detail.reviews().stream()
                .filter(review -> "APP".equals(review.source()))
                .findFirst()
                .orElseThrow();
        assertThat(appReview.score()).isEqualTo(9);
        assertThat(appReview.recommended()).isNull();
        assertThat(appReview.canEdit()).isTrue();
        assertThat(appReview.canVote()).isFalse();

        GameReviewResponse syncedSteamReview = detail.reviews().stream()
                .filter(review -> "STEAM".equals(review.source()))
                .findFirst()
                .orElseThrow();
        assertThat(syncedSteamReview.score()).isNull();
        assertThat(syncedSteamReview.recommended()).isTrue();
        assertThat(syncedSteamReview.canEdit()).isFalse();
        assertThat(syncedSteamReview.canVote()).isFalse();
    }
}
