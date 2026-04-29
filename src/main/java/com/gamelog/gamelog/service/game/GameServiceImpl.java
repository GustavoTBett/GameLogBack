package com.gamelog.gamelog.service.game;

import com.gamelog.gamelog.controller.dto.GameDetailResponse;
import com.gamelog.gamelog.controller.dto.GameReviewResponse;
import com.gamelog.gamelog.exception.EntityCannotBeNull;
import com.gamelog.gamelog.model.EnumUser.GamePlatform;
import com.gamelog.gamelog.model.Game;
import com.gamelog.gamelog.model.GamePlatformMapping;
import com.gamelog.gamelog.model.Rating;
import com.gamelog.gamelog.repository.GamePlatformMappingRepository;
import com.gamelog.gamelog.repository.GameRepository;
import com.gamelog.gamelog.controller.dto.GameSummaryResponse;
import com.gamelog.gamelog.model.GameGenre;
import com.gamelog.gamelog.repository.GameGenreRepository;
import com.gamelog.gamelog.repository.RatingRepository;
import com.gamelog.gamelog.service.image.RawgImageBackfillService;
import com.gamelog.gamelog.service.image.GameImageResolver;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Subquery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GameServiceImpl implements GameService{

    private static final Logger log = LoggerFactory.getLogger(GameServiceImpl.class);

    private final GameRepository gameRepository;
    private final GamePlatformMappingRepository gamePlatformRepository;
    private final GameGenreRepository gameGenreRepository;
    private final RatingRepository ratingRepository;
    private final GameImageResolver gameImageResolver;
    private final RawgImageBackfillService rawgImageBackfillService;

    public GameServiceImpl(
            GameRepository gameRepository,
            GamePlatformMappingRepository gamePlatformRepository,
            GameGenreRepository gameGenreRepository,
            RatingRepository ratingRepository,
            GameImageResolver gameImageResolver,
            RawgImageBackfillService rawgImageBackfillService
    ) {
        this.gameRepository = gameRepository;
        this.gamePlatformRepository = gamePlatformRepository;
        this.gameGenreRepository = gameGenreRepository;
        this.ratingRepository = ratingRepository;
        this.gameImageResolver = gameImageResolver;
        this.rawgImageBackfillService = rawgImageBackfillService;
    }

    @Override
    public Game save(Game game) {
        return gameRepository.save(game);
    }

    @Override
    public Optional<Game> get(Long id) {
        return gameRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GameDetailResponse> getSummaryBySlug(String slug) {
        log.info("Game detail requested by slug={}", slug);

        return gameRepository.findBySlug(slug)
                .map(game -> {
                    boolean hadDescriptionBefore = org.springframework.util.StringUtils.hasText(game.getDescription());
                    boolean hadRawgImageBefore = org.springframework.util.StringUtils.hasText(game.getRawgImageUrl());
                    boolean hadRawgSyncBefore = game.getImageLastCheckedAt() != null;

                log.info(
                    "Before RAWG sync slug={} gameId={} hasDescription={} hasRawgImage={} hasRawgSync={}",
                    slug,
                    game.getId(),
                    hadDescriptionBefore,
                    hadRawgImageBefore,
                    hadRawgSyncBefore
                );

                    gameImageResolver.enrichRawgMetadataIfMissing(game);

                    boolean hasDescriptionAfter = org.springframework.util.StringUtils.hasText(game.getDescription());
                    boolean hasRawgImageAfter = org.springframework.util.StringUtils.hasText(game.getRawgImageUrl());
                    boolean hasRawgSyncAfter = game.getImageLastCheckedAt() != null;

                log.info(
                    "After RAWG sync slug={} gameId={} hasDescription={} hasRawgImage={} hasRawgSync={}",
                    slug,
                    game.getId(),
                    hasDescriptionAfter,
                    hasRawgImageAfter,
                    hasRawgSyncAfter
                );

                    return toDetailResponse(game);
                });
    }

    @Override
    public void delete(Game game) {
        gameRepository.delete(game);
    }

    @Override
    public GamePlatformMapping addPlatform(Long gameId, GamePlatform platform) {
        Game game = get(gameId)
                .orElseThrow(() -> new EntityCannotBeNull("Game not found"));
        
        return gamePlatformRepository.save(GamePlatformMapping.builder()
                .game(game)
                .platform(platform)
                .build());
    }

    @Override
    public void removePlatform(Long gameId, GamePlatform platform) {
        gamePlatformRepository.deleteByGameIdAndPlatform(gameId, platform);
    }

    @Override
    public Set<GamePlatformMapping> getPlatforms(Long gameId) {
        Game game = get(gameId)
                .orElseThrow(() -> new EntityCannotBeNull("Game not found"));
        return game.getPlatforms() != null ? game.getPlatforms() : new HashSet<>();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GameSummaryResponse> explore(int page, int size, Long genreId, GamePlatform platform, Double minRating) {
        Sort sort = Sort.by(Sort.Order.desc("averageRating"), Sort.Order.asc("name"));
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Game> specification = null;
        
        if (minRating != null) {
            specification = Specification.where(minRatingSpec(minRating));
        }
        
        if (platform != null) {
            if (specification == null) {
                specification = Specification.where(platformSpec(platform));
            } else {
                specification = specification.and(platformSpec(platform));
            }
        }
        
        if (genreId != null) {
            if (specification == null) {
                specification = Specification.where(genreSpec(genreId));
            } else {
                specification = specification.and(genreSpec(genreId));
            }
        }

        Page<Game> gamesPage;
        if (specification == null) {
            gamesPage = gameRepository.findAll(pageable);
        } else {
            gamesPage = gameRepository.findAll(specification, pageable);
        }
        
        List<GameSummaryResponse> summaries = mapGames(gamesPage.getContent());
        return new PageImpl<>(summaries, pageable, gamesPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public List<GameSummaryResponse> getPopular(int limit) {
        Page<Game> games = gameRepository.findPopular(PageRequest.of(0, limit));
        return mapGames(games.getContent());
    }

    @Override
    @Transactional(readOnly = true)
    public List<GameSummaryResponse> getTopRated(int limit) {
        Pageable pageable = PageRequest.of(
                0,
                limit,
                Sort.by(Sort.Order.desc("averageRating"), Sort.Order.asc("name"))
        );
        Page<Game> games = gameRepository.findAll(pageable);
        return mapGames(games.getContent());
    }

    private Specification<Game> minRatingSpec(Double minRating) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("averageRating"), minRating);
    }

    private Specification<Game> platformSpec(GamePlatform platform) {
        return (root, query, cb) -> {
            query.distinct(true);
            Join<Game, GamePlatformMapping> join = root.join("platforms", JoinType.INNER);
            return cb.equal(join.get("platform"), platform);
        };
    }

    private Specification<Game> genreSpec(Long genreId) {
        return (root, query, cb) -> {
            query.distinct(true);
            Subquery<Long> subquery = query.subquery(Long.class);
            var gameGenreRoot = subquery.from(GameGenre.class);
            subquery.select(gameGenreRoot.get("game").get("id"));
            subquery.where(cb.equal(gameGenreRoot.get("genre").get("id"), genreId));
            return root.get("id").in(subquery);
        };
    }

    private List<GameSummaryResponse> mapGames(List<Game> games) {
        if (games.isEmpty()) {
            return List.of();
        }

        rawgImageBackfillService.triggerBackfillForGames(games);

        Map<Long, Long> reviewsByGame = getReviewsByGame(games);
        Map<Long, List<String>> genresByGame = getGenresByGame(games);
        Map<Long, List<GamePlatform>> platformsByGame = getPlatformsByGame(games);

        return games.stream()
                .map(game -> toSummaryResponse(game, reviewsByGame, genresByGame, platformsByGame))
                .toList();
    }

    private GameSummaryResponse toSummaryResponse(Game game) {
        return toSummaryResponse(
                game,
                Map.of(game.getId(), 0L),
                Map.of(game.getId(), List.of()),
                Map.of(game.getId(), List.of())
        );
    }

    private GameDetailResponse toDetailResponse(Game game) {
        Map<Long, Long> reviewsByGame = getReviewsByGame(List.of(game));
        Map<Long, List<String>> genresByGame = getGenresByGame(List.of(game));
        Map<Long, List<GamePlatform>> platformsByGame = getPlatformsByGame(List.of(game));

        GameSummaryResponse summary = toSummaryResponse(game, reviewsByGame, genresByGame, platformsByGame);
        List<GameReviewResponse> reviews = ratingRepository.findAllByGameIdOrderByCreatedAtDescIdDesc(game.getId())
                .stream()
                .map(this::toGameReviewResponse)
                .toList();

        return new GameDetailResponse(
            summary.id(),
            summary.name(),
            summary.slug(),
            summary.description(),
            summary.descriptionPtBr(),
            summary.coverUrl(),
            summary.averageRating(),
            summary.defaultRating(),
            summary.releaseDate(),
            summary.developer(),
            summary.totalReviews(),
            summary.genres(),
            summary.platforms(),
            reviews
        );
    }

    private GameReviewResponse toGameReviewResponse(Rating rating) {
        String username = rating.getUser() != null ? rating.getUser().getUsername() : "Usuário";

        return new GameReviewResponse(
                rating.getId(),
                rating.getScore(),
                rating.getReview(),
                username,
                rating.getCreatedAt()
        );
    }

    private GameSummaryResponse toSummaryResponse(
            Game game,
            Map<Long, Long> reviewsByGame,
            Map<Long, List<String>> genresByGame,
            Map<Long, List<GamePlatform>> platformsByGame
    ) {
        String resolvedCoverUrl = gameImageResolver.resolveAndPersistCoverUrl(game);

        return new GameSummaryResponse(
            game.getId(),
            game.getName(),
            game.getSlug(),
            game.getDescription(),
            game.getDescriptionPtBr(),
            resolvedCoverUrl,
            game.getAverageRating(),
            game.getDefaultRating(),
            game.getReleaseDate(),
            game.getDeveloper(),
            reviewsByGame.getOrDefault(game.getId(), 0L),
            genresByGame.getOrDefault(game.getId(), List.of()),
            platformsByGame.getOrDefault(game.getId(), List.of())
        );
    }

    private Map<Long, Long> getReviewsByGame(Collection<Game> games) {
        Set<Long> gameIds = games.stream().map(Game::getId).collect(Collectors.toSet());
        Map<Long, Long> reviewsByGame = new HashMap<>();
        ratingRepository.countRatingsByGameIds(gameIds)
                .forEach(tuple -> reviewsByGame.put((Long) tuple[0], (Long) tuple[1]));
        return reviewsByGame;
    }

    private Map<Long, List<String>> getGenresByGame(Collection<Game> games) {
        Set<Long> gameIds = games.stream().map(Game::getId).collect(Collectors.toSet());
        Map<Long, List<String>> genresByGame = new HashMap<>();

        gameGenreRepository.findAllByGameIdsWithGenre(gameIds)
                .forEach(mapping -> {
                    Long gameId = mapping.getGame().getId();
                    genresByGame.computeIfAbsent(gameId, ignored -> new ArrayList<>())
                            .add(mapping.getGenre().getName());
                });

        genresByGame.replaceAll((ignored, genres) -> genres.stream().sorted().toList());
        return genresByGame;
    }

    private Map<Long, List<GamePlatform>> getPlatformsByGame(Collection<Game> games) {
        Set<Long> gameIds = games.stream().map(Game::getId).collect(Collectors.toSet());
        Map<Long, List<GamePlatform>> platformsByGame = new HashMap<>();

        gamePlatformRepository.findAllByGameIdIn(gameIds)
                .forEach(mapping -> {
                    Long gameId = mapping.getGame().getId();
                    platformsByGame.computeIfAbsent(gameId, ignored -> new ArrayList<>())
                            .add(mapping.getPlatform());
                });

        platformsByGame.replaceAll((ignored, platforms) -> platforms.stream().sorted().toList());
        return platformsByGame;
    }
}
