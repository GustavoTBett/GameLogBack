package com.gamelog.gamelog.service.recommendation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamelog.gamelog.ai.AiClient;
import com.gamelog.gamelog.controller.dto.AiRecommendationCandidatesResponse;
import com.gamelog.gamelog.controller.dto.AiRecommendationResponse;
import com.gamelog.gamelog.controller.dto.RecommendationResponse;
import com.gamelog.gamelog.exception.InsufficientRatingsException;
import com.gamelog.gamelog.model.Favorite;
import com.gamelog.gamelog.model.Game;
import com.gamelog.gamelog.model.Rating;
import com.gamelog.gamelog.model.User;
import com.gamelog.gamelog.model.UserPlatformMapping;
import com.gamelog.gamelog.repository.FavoriteRepository;
import com.gamelog.gamelog.repository.GameGenreRepository;
import com.gamelog.gamelog.repository.GamePlatformMappingRepository;
import com.gamelog.gamelog.repository.GameRepository;
import com.gamelog.gamelog.repository.RatingRepository;
import com.gamelog.gamelog.repository.UserPlatformMappingRepository;
import com.gamelog.gamelog.service.image.GameImageResolver;
import com.gamelog.gamelog.service.user.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RecommendationGenerationOrchestrator {

    private static final int MIN_RATINGS_REQUIRED = 3;
    private static final int MAX_AI_ATTEMPTS = 3;
    private static final int MAX_CANDIDATES_PER_ATTEMPT = 3;
    private static final int RECENT_ITEMS_LIMIT = 5;
    private static final int AVOID_LIST_LIMIT = 40;
    private static final int PROMPT_EXCLUSION_LIMIT = 20;
    private static final int REASON_MAX_LENGTH = 150;
    private static final int TOP_PREF_LIMIT = 5;

    private final RatingRepository ratingRepository;
    private final FavoriteRepository favoriteRepository;
    private final UserPlatformMappingRepository userPlatformMappingRepository;
    private final GameGenreRepository gameGenreRepository;
    private final GamePlatformMappingRepository gamePlatformMappingRepository;
    private final GameRepository gameRepository;
    private final UserService userService;
    private final AiClient aiClient;
    private final ObjectMapper objectMapper;
    private final RawgGameImportService rawgGameImportService;
    private final GameImageResolver gameImageResolver;

    public RecommendationGenerationOrchestrator(
            RatingRepository ratingRepository,
            FavoriteRepository favoriteRepository,
            UserPlatformMappingRepository userPlatformMappingRepository,
            GameGenreRepository gameGenreRepository,
            GamePlatformMappingRepository gamePlatformMappingRepository,
            GameRepository gameRepository,
            UserService userService,
            AiClient aiClient,
            ObjectMapper objectMapper,
            RawgGameImportService rawgGameImportService,
            GameImageResolver gameImageResolver
    ) {
        this.ratingRepository = ratingRepository;
        this.favoriteRepository = favoriteRepository;
        this.userPlatformMappingRepository = userPlatformMappingRepository;
        this.gameGenreRepository = gameGenreRepository;
        this.gamePlatformMappingRepository = gamePlatformMappingRepository;
        this.gameRepository = gameRepository;
        this.userService = userService;
        this.aiClient = aiClient;
        this.objectMapper = objectMapper;
        this.rawgGameImportService = rawgGameImportService;
        this.gameImageResolver = gameImageResolver;
    }

    @Transactional
    public RecommendationResponse generateRecommendation(Long userId) {
        User user = userService.get(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        List<Rating> userRatings = ratingRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(userId);
        if (userRatings.size() < MIN_RATINGS_REQUIRED) {
            throw new InsufficientRatingsException(
                    String.format("User has %d ratings but needs at least %d to generate recommendations",
                            userRatings.size(), MIN_RATINGS_REQUIRED)
            );
        }

        List<Favorite> userFavorites = favoriteRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(userId);
        List<UserPlatformMapping> userPlatforms = userPlatformMappingRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(userId);

        UserPreferencesAnalysis analysis = analyzeUserPreferences(user, userRatings, userFavorites, userPlatforms);

        Set<String> excludedKeys = new LinkedHashSet<>(analysis.exclusionKeys());
        Set<String> excludedNames = new LinkedHashSet<>(analysis.excludedGameNames());
        Set<String> excludedSlugs = new LinkedHashSet<>(analysis.excludedGameSlugs());

        for (int attempt = 1; attempt <= MAX_AI_ATTEMPTS; attempt++) {
            String prompt = buildPrompt(analysis, excludedNames, excludedSlugs);
            String aiResponse = callAiProvider(prompt);
            AiRecommendationCandidatesResponse recommendationBatch = parseAiResponse(aiResponse);

            Optional<RecommendationResponse> selected = selectRecommendation(
                    userId,
                    recommendationBatch,
                    excludedKeys,
                    excludedNames,
                    excludedSlugs
            );

            if (selected.isPresent()) {
                return selected.get();
            }

            log.warn("No valid recommendation candidates found on attempt {} for user {}", attempt, userId);
        }

        throw new RuntimeException("Falha ao gerar uma recomendacao valida apos varias tentativas");
    }

    private UserPreferencesAnalysis analyzeUserPreferences(
            User user,
            List<Rating> ratings,
            List<Favorite> favorites,
            List<UserPlatformMapping> userPlatforms
    ) {
        Set<Long> signalGameIds = new LinkedHashSet<>();
        ratings.forEach(rating -> signalGameIds.add(rating.getGame().getId()));
        favorites.forEach(favorite -> signalGameIds.add(favorite.getGame().getId()));

        Map<Long, List<String>> genresByGameId = loadGenresByGameId(signalGameIds);
        Map<Long, List<String>> platformsByGameId = loadPlatformsByGameId(signalGameIds);

        Map<String, Integer> genreCounts = countGenresByGameIds(signalGameIds);
        Map<String, Integer> platformCounts = countPlatformsByGameIds(signalGameIds);

        List<String> ownedPlatforms = userPlatforms.stream()
                .map(mapping -> mapping.getPlatform().name())
                .distinct()
                .sorted()
                .toList();

        double averageUserRating = ratings.stream()
                .mapToInt(Rating::getScore)
                .average()
                .orElse(0.0);

        List<Map<String, Object>> recentRatings = ratings.stream()
                .limit(RECENT_ITEMS_LIMIT)
                .map(rating -> buildGameContextEntry(
                        rating.getGame(),
                        "rating",
                        rating.getScore(),
                        rating.getCreatedAt(),
                        genresByGameId,
                        platformsByGameId
                ))
                .toList();

        List<Map<String, Object>> recentFavorites = favorites.stream()
                .limit(RECENT_ITEMS_LIMIT)
                .map(favorite -> buildGameContextEntry(
                        favorite.getGame(),
                        "favorite",
                        null,
                        favorite.getCreatedAt(),
                        genresByGameId,
                        platformsByGameId
                ))
                .toList();

        List<Map<String, Object>> avoidGames = buildAvoidGameEntries(ratings, favorites, genresByGameId, platformsByGameId);

        Set<String> exclusionKeys = new LinkedHashSet<>();
        Set<String> excludedGameNames = new LinkedHashSet<>();
        Set<String> excludedGameSlugs = new LinkedHashSet<>();

        ratings.forEach(rating -> addGameToExclusions(rating.getGame(), exclusionKeys, excludedGameNames, excludedGameSlugs));
        favorites.forEach(favorite -> addGameToExclusions(favorite.getGame(), exclusionKeys, excludedGameNames, excludedGameSlugs));

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("username", user.getUsername());
        if (user.getBio() != null && !user.getBio().isBlank()) {
            profile.put("bio", user.getBio());
        }
        profile.put("ratings_count", ratings.size());
        profile.put("favorites_count", favorites.size());
        profile.put("average_score", roundTwoDecimals(averageUserRating));
        profile.put("top_genres", buildRankedEntries(genreCounts, TOP_PREF_LIMIT));
        profile.put("top_platforms", buildRankedEntries(platformCounts, TOP_PREF_LIMIT));
        profile.put("owned_platforms", ownedPlatforms);
        profile.put("recent_ratings", recentRatings);
        profile.put("recent_favorites", recentFavorites);
        profile.put("avoid_games", avoidGames);

        try {
            String profileJson = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(profile);

            String summary = String.format(
                    Locale.ROOT,
                    "Usuário com %d avaliações e %d favoritos. Média de nota %.2f. Gêneros mais fortes: %s. Plataformas mais vistas nos jogos: %s. Plataformas declaradas: %s%s",
                    ratings.size(),
                    favorites.size(),
                    averageUserRating,
                    formatTopEntries(genreCounts, TOP_PREF_LIMIT),
                    formatTopEntries(platformCounts, TOP_PREF_LIMIT),
                    ownedPlatforms.isEmpty() ? "nenhuma informada" : String.join(", ", ownedPlatforms),
                    user.getBio() == null || user.getBio().isBlank() ? "" : String.format(". Bio do usuário: %s", user.getBio())
            );

            return new UserPreferencesAnalysis(
                    summary,
                    profileJson,
                    exclusionKeys,
                    excludedGameNames,
                    excludedGameSlugs
            );
        } catch (Exception e) {
            log.error("Erro ao serializar contexto de recomendacao", e);
            throw new RuntimeException("Erro ao analisar preferências do usuário", e);
        }
    }

    private String buildPrompt(
            UserPreferencesAnalysis analysis,
            Set<String> excludedGameNames,
            Set<String> excludedGameSlugs
    ) {
        return String.format(
                Locale.ROOT,
                """
                        Você é um especialista em recomendações de videogames. Use o contexto estruturado do usuário para sugerir jogos realmente aderentes ao perfil.

                        Regras obrigatórias:
                        - Retorne exatamente %d candidatos únicos.
                        - Não repita jogos já avaliados, favoritados ou bloqueados nesta tentativa.
                        - Prefira jogos alinhados aos gêneros e plataformas mais frequentes.
                        - Se houver dúvida, priorize jogos próximos dos favoritos recentes.
                        - Responda somente com JSON válido, sem markdown e sem qualquer texto extra.
                        - Cada reason deve ter no máximo %d caracteres.

                        Resumo interpretado:
                        %s

                        Contexto estruturado do usuário:
                        %s

                        Exclusões adicionais desta tentativa por nome:
                        %s

                        Exclusões adicionais desta tentativa por slug:
                        %s

                        Formato de resposta esperado:
                        {
                          "candidates": [
                            {
                              "game_name": "Nome exato do jogo",
                              "game_slug": "slug-exato-no-rawg",
                              "reason": "Motivo breve"
                            },
                            {
                              "game_name": "Outro jogo",
                              "game_slug": "outro-slug",
                              "reason": "Motivo breve"
                            },
                            {
                              "game_name": "Terceiro jogo",
                              "game_slug": "terceiro-slug",
                              "reason": "Motivo breve"
                            }
                          ]
                        }
                        """,
                MAX_CANDIDATES_PER_ATTEMPT,
                REASON_MAX_LENGTH,
                analysis.summary(),
                analysis.profileJson(),
                joinValues(excludedGameNames, PROMPT_EXCLUSION_LIMIT),
                joinValues(excludedGameSlugs, PROMPT_EXCLUSION_LIMIT)
        );
    }

    private String callAiProvider(String prompt) {
        try {
            log.info("Enviando prompt para AI provider para gerar recomendacao");
            String response = aiClient.generate(prompt);
            log.info("Resposta recebida do AI provider");
            return response;
        } catch (Exception e) {
            log.error("Erro ao chamar AI provider", e);
            throw new RuntimeException("Falha ao gerar recomendação com IA", e);
        }
    }

    private AiRecommendationCandidatesResponse parseAiResponse(String response) {
        try {
            String cleanedResponse = cleanAiResponse(response);
            log.debug("Parsing resposta da IA: {}", cleanedResponse);

            if (cleanedResponse.startsWith("[")) {
                List<AiRecommendationResponse> candidates = objectMapper.readValue(
                        cleanedResponse,
                        new TypeReference<List<AiRecommendationResponse>>() {
                        }
                );
                return new AiRecommendationCandidatesResponse(candidates);
            }

            try {
                return objectMapper.readValue(cleanedResponse, AiRecommendationCandidatesResponse.class);
            } catch (Exception wrapperException) {
                AiRecommendationResponse singleCandidate = objectMapper.readValue(
                        cleanedResponse,
                        AiRecommendationResponse.class
                );
                return new AiRecommendationCandidatesResponse(List.of(singleCandidate));
            }
        } catch (Exception e) {
            log.error("Erro ao fazer parse da resposta da IA: {}", response, e);
            throw new RuntimeException("Falha ao processar resposta da IA", e);
        }
    }

    private Optional<RecommendationResponse> selectRecommendation(
            Long userId,
            AiRecommendationCandidatesResponse recommendationBatch,
            Set<String> excludedKeys,
            Set<String> excludedGameNames,
            Set<String> excludedGameSlugs
    ) {
        if (recommendationBatch == null || recommendationBatch.candidates() == null || recommendationBatch.candidates().isEmpty()) {
            return Optional.empty();
        }

        int processedCandidates = 0;
        for (AiRecommendationResponse candidate : recommendationBatch.candidates()) {
            if (processedCandidates >= MAX_CANDIDATES_PER_ATTEMPT) {
                break;
            }
            processedCandidates++;

            if (candidate == null) {
                continue;
            }

            String requestedSlug = resolveRequestedSlug(candidate);
            if (isBlank(requestedSlug) || isCandidateExcluded(candidate, requestedSlug, excludedKeys)) {
                addCandidateToExclusions(candidate, requestedSlug, excludedKeys, excludedGameNames, excludedGameSlugs);
                continue;
            }

            try {
                GameImportResult importResult = findOrImportGame(requestedSlug, candidate.gameName());
                Game recommendedGame = importResult.game();

                if (isGameAlreadyKnownForUser(userId, recommendedGame.getId())) {
                    addGameToExclusions(recommendedGame, excludedKeys, excludedGameNames, excludedGameSlugs);
                    log.info("Ignoring duplicate recommendation candidate '{}' for user {}", recommendedGame.getName(), userId);
                    continue;
                }

                return Optional.of(buildRecommendationResponse(recommendedGame, candidate.reason(), importResult.newlyImported()));
            } catch (Exception e) {
                addCandidateToExclusions(candidate, requestedSlug, excludedKeys, excludedGameNames, excludedGameSlugs);
                log.warn("Failed to materialize recommendation candidate '{}' for user {}", candidate.gameName(), userId, e);
            }
        }

        return Optional.empty();
    }

    private RecommendationResponse buildRecommendationResponse(Game recommendedGame, String reason, boolean isNewlyImported) {
        gameImageResolver.enrichRawgMetadataIfMissing(recommendedGame);
        String coverUrl = gameImageResolver.resolveAndPersistCoverUrl(recommendedGame);

        return new RecommendationResponse(
                recommendedGame.getName(),
                recommendedGame.getSlug(),
                truncateReason(reason),
                recommendedGame.getId(),
                recommendedGame.getAverageRating(),
                recommendedGame.getDefaultRating(),
                recommendedGame.getReleaseDate(),
                isNewlyImported,
                coverUrl,
                OffsetDateTime.now(ZoneOffset.UTC)
        );
    }

    private boolean isGameAlreadyKnownForUser(Long userId, Long gameId) {
        return ratingRepository.findFirstByUserIdAndGameId(userId, gameId).isPresent()
                || favoriteRepository.existsByUserIdAndGameId(userId, gameId);
    }

    private boolean isCandidateExcluded(AiRecommendationResponse candidate, String requestedSlug, Set<String> excludedKeys) {
        String candidateNameKey = normalizeSlug(candidate.gameName());
        String candidateSlugKey = normalizeSlug(requestedSlug);
        return excludedKeys.contains(candidateNameKey) || excludedKeys.contains(candidateSlugKey);
    }

    private void addCandidateToExclusions(
            AiRecommendationResponse candidate,
            String requestedSlug,
            Set<String> excludedKeys,
            Set<String> excludedGameNames,
            Set<String> excludedGameSlugs
    ) {
        if (candidate == null) {
            return;
        }

        addTextToExclusions(candidate.gameName(), excludedKeys, excludedGameNames);
        addTextToExclusions(candidate.gameSlug(), excludedKeys, excludedGameSlugs);
        addTextToExclusions(requestedSlug, excludedKeys, excludedGameSlugs);
    }

    private void addGameToExclusions(
            Game game,
            Set<String> excludedKeys,
            Set<String> excludedGameNames,
            Set<String> excludedGameSlugs
    ) {
        if (game == null) {
            return;
        }

        addTextToExclusions(game.getName(), excludedKeys, excludedGameNames);
        addTextToExclusions(game.getSlug(), excludedKeys, excludedGameSlugs);
    }

    private void addTextToExclusions(String value, Set<String> excludedKeys, Set<String> excludedValues) {
        if (isBlank(value)) {
            return;
        }

        String normalizedKey = normalizeSlug(value);
        if (!normalizedKey.isBlank()) {
            excludedKeys.add(normalizedKey);
        }

        excludedValues.add(value.trim());
    }

    private Map<Long, List<String>> loadGenresByGameId(Collection<Long> gameIds) {
        Map<Long, List<String>> genresByGameId = new HashMap<>();
        if (gameIds == null || gameIds.isEmpty()) {
            return genresByGameId;
        }

        gameGenreRepository.findAllByGameIdsWithGenre(gameIds)
                .forEach(mapping -> {
                    Long gameId = mapping.getGame().getId();
                    genresByGameId.computeIfAbsent(gameId, ignored -> new ArrayList<>())
                            .add(mapping.getGenre().getName());
                });

        genresByGameId.replaceAll((ignored, genres) -> genres.stream().distinct().sorted().toList());
        return genresByGameId;
    }

    private Map<Long, List<String>> loadPlatformsByGameId(Collection<Long> gameIds) {
        Map<Long, List<String>> platformsByGameId = new HashMap<>();
        if (gameIds == null || gameIds.isEmpty()) {
            return platformsByGameId;
        }

        gamePlatformMappingRepository.findAllByGameIdIn(gameIds)
                .forEach(mapping -> {
                    Long gameId = mapping.getGame().getId();
                    platformsByGameId.computeIfAbsent(gameId, ignored -> new ArrayList<>())
                            .add(mapping.getPlatform().name());
                });

        platformsByGameId.replaceAll((ignored, platforms) -> platforms.stream().distinct().sorted().toList());
        return platformsByGameId;
    }

    private Map<String, Integer> countGenresByGameIds(Collection<Long> gameIds) {
        Map<String, Integer> counts = new HashMap<>();
        if (gameIds == null || gameIds.isEmpty()) {
            return counts;
        }

        gameGenreRepository.findAllByGameIdsWithGenre(gameIds)
                .forEach(mapping -> incrementCount(counts, mapping.getGenre().getName()));

        return counts;
    }

    private Map<String, Integer> countPlatformsByGameIds(Collection<Long> gameIds) {
        Map<String, Integer> counts = new HashMap<>();
        if (gameIds == null || gameIds.isEmpty()) {
            return counts;
        }

        gamePlatformMappingRepository.findAllByGameIdIn(gameIds)
                .forEach(mapping -> incrementCount(counts, mapping.getPlatform().name()));

        return counts;
    }

    private void incrementCount(Map<String, Integer> counts, String value) {
        if (isBlank(value)) {
            return;
        }

        counts.merge(value.trim(), 1, Integer::sum);
    }

    private List<Map<String, Object>> buildRankedEntries(Map<String, Integer> counts, int limit) {
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry::getKey))
                .limit(Math.max(limit, 0))
                .map(entry -> {
                    Map<String, Object> rankedEntry = new LinkedHashMap<>();
                    rankedEntry.put("name", entry.getKey());
                    rankedEntry.put("count", entry.getValue());
                    return rankedEntry;
                })
                .collect(Collectors.toList());
    }

    private String formatTopEntries(Map<String, Integer> counts, int limit) {
        List<Map<String, Object>> rankedEntries = buildRankedEntries(counts, limit);
        if (rankedEntries.isEmpty()) {
            return "nenhum sinal forte identificado";
        }

        return rankedEntries.stream()
                .map(entry -> String.format(Locale.ROOT, "%s (%s)", entry.get("name"), entry.get("count")))
                .collect(Collectors.joining(", "));
    }

    private List<Map<String, Object>> buildAvoidGameEntries(
            List<Rating> ratings,
            List<Favorite> favorites,
            Map<Long, List<String>> genresByGameId,
            Map<Long, List<String>> platformsByGameId
    ) {
        LinkedHashMap<String, Map<String, Object>> entriesByKey = new LinkedHashMap<>();

        ratings.forEach(rating -> putGameContextEntry(
                entriesByKey,
                rating.getGame(),
                "rating",
                rating.getScore(),
                rating.getCreatedAt(),
                genresByGameId,
                platformsByGameId
        ));

        favorites.forEach(favorite -> putGameContextEntry(
                entriesByKey,
                favorite.getGame(),
                "favorite",
                null,
                favorite.getCreatedAt(),
                genresByGameId,
                platformsByGameId
        ));

        return entriesByKey.values().stream()
                .limit(AVOID_LIST_LIMIT)
                .collect(Collectors.toList());
    }

    private void putGameContextEntry(
            Map<String, Map<String, Object>> entriesByKey,
            Game game,
            String source,
            Integer score,
            Instant createdAt,
            Map<Long, List<String>> genresByGameId,
            Map<Long, List<String>> platformsByGameId
    ) {
        if (game == null) {
            return;
        }

        String key = buildGameKey(game);
        entriesByKey.computeIfAbsent(key, ignored -> buildGameContextEntry(
                game,
                source,
                score,
                createdAt,
                genresByGameId,
                platformsByGameId
        ));
    }

    private Map<String, Object> buildGameContextEntry(
            Game game,
            String source,
            Integer score,
            Instant createdAt,
            Map<Long, List<String>> genresByGameId,
            Map<Long, List<String>> platformsByGameId
    ) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("game_name", game.getName());
        entry.put("game_slug", game.getSlug());
        entry.put("source", source);

        if (score != null) {
            entry.put("score", score);
        }

        if (createdAt != null) {
            entry.put("created_at", createdAt.toString());
        }

        if (game.getDeveloper() != null && !game.getDeveloper().isBlank()) {
            entry.put("developer", game.getDeveloper());
        }

        if (game.getPublisher() != null && !game.getPublisher().isBlank()) {
            entry.put("publisher", game.getPublisher());
        }

        entry.put("genres", genresByGameId.getOrDefault(game.getId(), List.of()));
        entry.put("platforms", platformsByGameId.getOrDefault(game.getId(), List.of()));
        return entry;
    }

    private String buildGameKey(Game game) {
        String normalizedSlug = normalizeSlug(game.getSlug());
        if (!normalizedSlug.isBlank()) {
            return normalizedSlug;
        }

        return normalizeSlug(game.getName());
    }

    private String joinValues(Collection<String> values, int limit) {
        List<String> limitedValues = limitValues(values, limit);
        if (limitedValues.isEmpty()) {
            return "nenhuma";
        }

        return String.join(", ", limitedValues);
    }

    private List<String> limitValues(Collection<String> values, int limit) {
        if (values == null || values.isEmpty() || limit <= 0) {
            return List.of();
        }

        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .limit(limit)
                .collect(Collectors.toList());
    }

    private String cleanAiResponse(String response) {
        if (response == null) {
            return "";
        }

        String cleanedResponse = response
                .replaceAll("```json", "")
                .replaceAll("```", "")
                .trim();

        int arrayStart = cleanedResponse.indexOf('[');
        int arrayEnd = cleanedResponse.lastIndexOf(']');
        if (arrayStart >= 0 && arrayEnd > arrayStart) {
            return cleanedResponse.substring(arrayStart, arrayEnd + 1).trim();
        }

        int objectStart = cleanedResponse.indexOf('{');
        int objectEnd = cleanedResponse.lastIndexOf('}');
        if (objectStart >= 0 && objectEnd > objectStart) {
            return cleanedResponse.substring(objectStart, objectEnd + 1).trim();
        }

        return cleanedResponse;
    }

    private GameImportResult findOrImportGame(String gameSlug, String gameName) {
        Optional<Game> existingGame = isBlank(gameSlug) ? Optional.empty() : gameRepository.findBySlug(gameSlug);
        if (existingGame.isPresent()) {
            return new GameImportResult(existingGame.get(), false);
        }

        try {
            log.info("Buscando jogo '{}' no RAWG usando slug '{}'", gameName, gameSlug);
            Game importedGame = rawgGameImportService.importGameBySlug(gameSlug);
            log.info("Jogo '{}' sincronizado com sucesso. ID: {}", gameName, importedGame.getId());
            return new GameImportResult(importedGame, true);
        } catch (Exception slugException) {
            log.warn("Falha ao buscar jogo por slug '{}'. Tentando por nome '{}'", gameSlug, gameName, slugException);

            Game importedGame = rawgGameImportService.importGameByName(gameName);
            log.info("Jogo '{}' sincronizado por nome com sucesso. ID: {}", gameName, importedGame.getId());
            return new GameImportResult(importedGame, true);
        }
    }

    private String resolveRequestedSlug(AiRecommendationResponse recommendation) {
        if (recommendation != null && recommendation.gameSlug() != null && !recommendation.gameSlug().isBlank()) {
            return normalizeSlug(recommendation.gameSlug());
        }

        return normalizeSlug(recommendation != null ? recommendation.gameName() : null);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String truncateReason(String reason) {
        if (isBlank(reason)) {
            return "Motivo indisponivel";
        }

        String trimmed = reason.trim();
        if (trimmed.length() <= REASON_MAX_LENGTH) {
            return trimmed;
        }

        return trimmed.substring(0, REASON_MAX_LENGTH).trim();
    }

    private double roundTwoDecimals(double value) {
        return Math.round(value * 100.0d) / 100.0d;
    }

    private String normalizeSlug(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        return normalized.isBlank() ? value.toLowerCase(Locale.ROOT).trim() : normalized;
    }

    private record UserPreferencesAnalysis(
            String summary,
            String profileJson,
            Set<String> exclusionKeys,
            Set<String> excludedGameNames,
            Set<String> excludedGameSlugs
    ) {
    }

    private record GameImportResult(Game game, boolean newlyImported) {
    }
}
