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
}
