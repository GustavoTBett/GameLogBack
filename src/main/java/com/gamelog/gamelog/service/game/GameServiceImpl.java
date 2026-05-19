package com.gamelog.gamelog.service.game;

import com.gamelog.gamelog.controller.dto.GameDetailResponse;
import com.gamelog.gamelog.controller.dto.GameReviewResponse;
import com.gamelog.gamelog.exception.EntityCannotBeNull;
import com.gamelog.gamelog.model.enums.GamePlatform;
import com.gamelog.gamelog.model.enums.RatingVoteType;
import com.gamelog.gamelog.model.Game;
import com.gamelog.gamelog.model.GamePlatformMapping;
import com.gamelog.gamelog.model.Rating;
import com.gamelog.gamelog.model.SteamUserReview;
import com.gamelog.gamelog.repository.GamePlatformMappingRepository;
import com.gamelog.gamelog.repository.GameRepository;
import com.gamelog.gamelog.controller.dto.GameSummaryResponse;
import com.gamelog.gamelog.model.GameGenre;
import com.gamelog.gamelog.repository.GameGenreRepository;
import com.gamelog.gamelog.repository.RatingRepository;
import com.gamelog.gamelog.repository.SteamUserReviewRepository;
import com.gamelog.gamelog.service.ratingvote.RatingVoteService;
import com.gamelog.gamelog.service.ratingvote.RatingVoteStats;
import com.gamelog.gamelog.service.image.RawgImageBackfillService;
import com.gamelog.gamelog.service.image.GameImageResolver;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
public class GameServiceImpl implements GameService{

    private static final Logger log = LoggerFactory.getLogger(GameServiceImpl.class);

    private final GameRepository gameRepository;
    private final GamePlatformMappingRepository gamePlatformRepository;
    private final GameGenreRepository gameGenreRepository;
    private final RatingRepository ratingRepository;
    private final SteamUserReviewRepository steamUserReviewRepository;
    @Lazy
    private final RatingVoteService ratingVoteService;
    private final GameImageResolver gameImageResolver;
    private final RawgImageBackfillService rawgImageBackfillService;

    public GameServiceImpl(
            GameRepository gameRepository,
            GamePlatformMappingRepository gamePlatformRepository,
            GameGenreRepository gameGenreRepository,
            RatingRepository ratingRepository,
            SteamUserReviewRepository steamUserReviewRepository,
            RatingVoteService ratingVoteService,
            GameImageResolver gameImageResolver,
            RawgImageBackfillService rawgImageBackfillService
    ) {
        this.gameRepository = gameRepository;
        this.gamePlatformRepository = gamePlatformRepository;
        this.gameGenreRepository = gameGenreRepository;
        this.ratingRepository = ratingRepository;
        this.steamUserReviewRepository = steamUserReviewRepository;
        this.ratingVoteService = ratingVoteService;
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
    public Optional<GameDetailResponse> getSummaryBySlug(String slug, Long currentUserId) {
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

                    return toDetailResponse(game, currentUserId);
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
    public Page<GameSummaryResponse> explore(int page, int size, Long genreId, GamePlatform platform, Double minRating, String q) {
        Sort sort = Sort.by(Sort.Order.desc("averageRating"), Sort.Order.asc("name"));
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Game> specification = null;

        if (q != null && !q.isBlank()) {
            specification = Specification.where(searchSpec(q));
        }
        
        if (minRating != null) {
            specification = specification == null
                    ? Specification.where(minRatingSpec(minRating))
                    : specification.and(minRatingSpec(minRating));
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
    public List<GameSummaryResponse> summarize(List<Game> games) {
        return mapGames(games);
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

    private Specification<Game> searchSpec(String queryText) {
        return (root, query, cb) -> {
            String normalizedQuery = "%" + queryText.toLowerCase().trim() + "%";

            Predicate nameMatches = cb.like(cb.lower(cb.coalesce(root.get("name"), "")), normalizedQuery);
            Predicate slugMatches = cb.like(cb.lower(cb.coalesce(root.get("slug"), "")), normalizedQuery);
            Predicate descriptionMatches = cb.like(cb.lower(cb.coalesce(root.get("description"), "")), normalizedQuery);
            Predicate descriptionPtBrMatches = cb.like(cb.lower(cb.coalesce(root.get("descriptionPtBr"), "")), normalizedQuery);
            Predicate developerMatches = cb.like(cb.lower(cb.coalesce(root.get("developer"), "")), normalizedQuery);
            Predicate publisherMatches = cb.like(cb.lower(cb.coalesce(root.get("publisher"), "")), normalizedQuery);

            return cb.or(
                    nameMatches,
                    slugMatches,
                    descriptionMatches,
                    descriptionPtBrMatches,
                    developerMatches,
                    publisherMatches
            );
        };
    }

    private List<GameSummaryResponse> mapGames(List<Game> games) {
        if (games.isEmpty()) {
            return List.of();
        }

        rawgImageBackfillService.triggerBackfillForGames(games);

        Map<Long, Long> appReviewsByGame = getAppReviewsByGame(games);
        Map<Long, Long> steamReviewsByGame = getSteamReviewsByGame(games);
        Map<Long, List<String>> genresByGame = getGenresByGame(games);
        Map<Long, List<GamePlatform>> platformsByGame = getPlatformsByGame(games);

        return games.stream()
                .map(game -> toSummaryResponse(game, appReviewsByGame, steamReviewsByGame, genresByGame, platformsByGame))
                .toList();
    }

    private GameSummaryResponse toSummaryResponse(Game game) {
        return toSummaryResponse(
                game,
                Map.of(game.getId(), 0L),
                Map.of(game.getId(), 0L),
                Map.of(game.getId(), List.of()),
                Map.of(game.getId(), List.of())
        );
    }

        private GameDetailResponse toDetailResponse(Game game, Long currentUserId) {
        Map<Long, Long> appReviewsByGame = getAppReviewsByGame(List.of(game));
        Map<Long, Long> steamReviewsByGame = getSteamReviewsByGame(List.of(game));
        Map<Long, List<String>> genresByGame = getGenresByGame(List.of(game));
        Map<Long, List<GamePlatform>> platformsByGame = getPlatformsByGame(List.of(game));

        GameSummaryResponse summary = toSummaryResponse(game, appReviewsByGame, steamReviewsByGame, genresByGame, platformsByGame);
        List<Rating> ratings = ratingRepository.findAllByGameIdOrderByCreatedAtDescIdDesc(game.getId());
        List<SteamUserReview> steamReviews = steamUserReviewRepository.findAllByGameIdAndActiveTrue(game.getId());
        Map<Long, RatingVoteStats> voteStatsByRating = ratingVoteService.getVoteStatsByRatingIds(
            ratings.stream().map(Rating::getId).collect(Collectors.toSet())
        );
        Map<Long, RatingVoteType> userVotesByRating = ratingVoteService.getUserVotesByRatingIds(
            ratings.stream().map(Rating::getId).collect(Collectors.toSet()),
            currentUserId
        );

        List<GameReviewResponse> reviews = ratings.stream()
            .map(rating -> toGameReviewResponse(
                rating,
                voteStatsByRating.getOrDefault(rating.getId(), RatingVoteStats.empty()),
                userVotesByRating.get(rating.getId()),
                currentUserId
            ))
            .collect(Collectors.toCollection(ArrayList::new));

        reviews.addAll(
            steamReviews.stream()
                .map(this::toGameReviewResponse)
                .toList()
        );

        reviews = reviews.stream()
            .sorted(Comparator
                .comparingLong((GameReviewResponse review) -> review.upvoteCount() - review.downvoteCount())
                .reversed()
                .thenComparing(GameReviewResponse::createdAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(GameReviewResponse::id, Comparator.nullsLast(Comparator.reverseOrder())))
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
            summary.appReviewCount(),
            summary.steamReviewCount(),
            summary.genres(),
            summary.platforms(),
            reviews
        );
    }

    private GameReviewResponse toGameReviewResponse(
            Rating rating,
            RatingVoteStats voteStats,
            RatingVoteType userVote,
            Long currentUserId
    ) {
        String username = rating.getUser() != null ? rating.getUser().getUsername() : "Usuário";

        Long ownerId = rating.getUser() != null ? rating.getUser().getId() : null;
        boolean canEdit = currentUserId != null && Objects.equals(ownerId, currentUserId);
        boolean canVote = currentUserId != null && ownerId != null && !Objects.equals(ownerId, currentUserId);

        return new GameReviewResponse(
                rating.getId(),
                rating.getScore(),
                rating.getReview(),
                username,
                rating.getCreatedAt(),
                rating.getUpdatedAt(),
                voteStats.upvoteCount(),
                voteStats.downvoteCount(),
                userVote != null ? userVote.name() : null,
                "APP",
                null,
                canEdit,
                canVote
        );
    }

    private GameReviewResponse toGameReviewResponse(SteamUserReview review) {
        String username = Optional.ofNullable(review.getSteamAccount())
                .map(com.gamelog.gamelog.model.SteamAccount::getUser)
                .map(com.gamelog.gamelog.model.User::getUsername)
                .orElse("Usuário Steam");

        Instant createdAt = review.getReviewedAt();
        Instant updatedAt = review.getReviewedAt();
        return new GameReviewResponse(
                review.getId(),
                null,
                review.getReviewText(),
                username,
                createdAt,
                updatedAt,
                0L,
                0L,
                null,
                "STEAM",
                review.getRecommended(),
                false,
                false
        );
    }

    private GameSummaryResponse toSummaryResponse(
            Game game,
            Map<Long, Long> appReviewsByGame,
            Map<Long, Long> steamReviewsByGame,
            Map<Long, List<String>> genresByGame,
            Map<Long, List<GamePlatform>> platformsByGame
    ) {
        String resolvedCoverUrl = gameImageResolver.resolveAndPersistCoverUrl(game);
        Long appReviewCount = appReviewsByGame.getOrDefault(game.getId(), 0L);
        Long steamReviewCount = steamReviewsByGame.getOrDefault(game.getId(), 0L);

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
            appReviewCount + steamReviewCount,
            appReviewCount,
            steamReviewCount,
            genresByGame.getOrDefault(game.getId(), List.of()),
            platformsByGame.getOrDefault(game.getId(), List.of())
        );
    }

    private Map<Long, Long> getAppReviewsByGame(Collection<Game> games) {
        Set<Long> gameIds = games.stream().map(Game::getId).collect(Collectors.toSet());
        Map<Long, Long> appReviewsByGame = new HashMap<>();

        ratingRepository.countRatingsByGameIds(gameIds)
                .forEach(tuple -> appReviewsByGame.put((Long) tuple[0], (Long) tuple[1]));

        return appReviewsByGame;
    }

    private Map<Long, Long> getSteamReviewsByGame(Collection<Game> games) {
        Set<Long> gameIds = games.stream().map(Game::getId).collect(Collectors.toSet());
        Map<Long, Long> steamReviewsByGame = new HashMap<>();

        steamUserReviewRepository.findAllByGameIdInAndActiveTrue(gameIds)
                .forEach(steamReview -> {
                    if (steamReview.getGame() == null || steamReview.getGame().getId() == null) {
                        return;
                    }

                    steamReviewsByGame.merge(steamReview.getGame().getId(), 1L, Long::sum);
                });

        return steamReviewsByGame;
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
