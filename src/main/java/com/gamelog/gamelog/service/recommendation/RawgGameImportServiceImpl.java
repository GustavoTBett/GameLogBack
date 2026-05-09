package com.gamelog.gamelog.service.recommendation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamelog.gamelog.model.enums.ImageSource;
import com.gamelog.gamelog.model.Game;
import com.gamelog.gamelog.repository.GameRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Locale;

@Service
@Slf4j
public class RawgGameImportServiceImpl implements RawgGameImportService {

    private final GameRepository gameRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${rawg.api.key:}")
    private String rawgApiKey;

    @Value("${rawg.api.base-url:https://api.rawg.io/api}")
    private String rawgBaseUrl;

    @Value("${rawg.api.timeout-ms:5000}")
    private int rawgTimeoutMs;

    @Value("${app.translation.base-url:}")
    private String translationBaseUrl;

    @Value("${app.translation.timeout-ms:15000}")
    private int translationTimeoutMs;

    @Value("${app.translation.connect-timeout-ms:5000}")
    private int translationConnectTimeoutMs;

    @Value("${app.translation.retry-count:1}")
    private int translationRetryCount;

    @Value("${app.translation.source-lang:en}")
    private String translationSourceLang;

    @Value("${app.translation.target-lang:pt}")
    private String translationTargetLang;

    public RawgGameImportServiceImpl(GameRepository gameRepository, ObjectMapper objectMapper) {
        this.gameRepository = gameRepository;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(5000))
                .build();
    }

    @Override
    public Game importGameByName(String gameName) {
        if (!StringUtils.hasText(rawgApiKey)) {
            log.warn("RAWG API key not configured. Cannot import game: {}", gameName);
            throw new RuntimeException("RAWG API key not configured");
        }

        try {
            log.info("Searching for game '{}' in RAWG API", gameName);
            JsonNode searchResults = callRawgSearch(gameName);

            if (searchResults == null || !searchResults.has("results") || searchResults.get("results").isEmpty()) {
                throw new RuntimeException("Game not found in RAWG: " + gameName);
            }

            JsonNode gameNode = searchResults.get("results").get(0);
            String slug = gameNode.path("slug").asText(null);
            if (!StringUtils.hasText(slug)) {
                slug = normalizeSlug(gameName);
            }

            if (!StringUtils.hasText(slug)) {
                throw new RuntimeException("RAWG search returned a game without slug: " + gameName);
            }

            return importGameBySlug(slug);
        } catch (Exception e) {
            log.error("Error importing game from RAWG: {}", gameName, e);
            throw new RuntimeException("Error importing game from RAWG: " + gameName, e);
        }
    }

    @Override
    public Game importGameBySlug(String slug) {
        if (!StringUtils.hasText(rawgApiKey)) {
            log.warn("RAWG API key not configured. Cannot import game by slug: {}", slug);
            throw new RuntimeException("RAWG API key not configured");
        }

        String normalizedSlug = normalizeSlug(slug);
        if (!StringUtils.hasText(normalizedSlug)) {
            throw new RuntimeException("Slug is required to import RAWG game");
        }

        try {
            log.info("Fetching RAWG game details by slug: {}", normalizedSlug);

            JsonNode gameDetails = callRawgGameDetailsBySlug(normalizedSlug);
            Game saved = persistRawgGame(gameDetails, normalizedSlug);

            log.info("Game '{}' imported successfully with ID {}", saved.getSlug(), saved.getId());
            return saved;
        } catch (Exception e) {
            log.error("Error importing game by slug: {}", normalizedSlug, e);
            throw new RuntimeException("Error importing game by slug: " + normalizedSlug, e);
        }
    }

    @Override
    public Game importGameByRawgId(Long rawgId) {
        if (!StringUtils.hasText(rawgApiKey)) {
            log.warn("RAWG API key not configured. Cannot import game by ID: {}", rawgId);
            throw new RuntimeException("RAWG API key not configured");
        }

        try {
            log.info("Fetching game details from RAWG API for rawgId: {}", rawgId);

            JsonNode gameDetails = callRawgGameDetailsById(rawgId);
            String slug = gameDetails.path("slug").asText(null);
            Game saved = persistRawgGame(gameDetails, slug);

            log.info("Game imported successfully from rawgId {} with ID {}", rawgId, saved.getId());
            return saved;
        } catch (Exception e) {
            log.error("Error importing game by rawgId: {}", rawgId, e);
            throw new RuntimeException("Error importing game by rawgId: " + rawgId, e);
        }
    }

    private JsonNode callRawgSearch(String gameName) throws Exception {
        String encodedName = URLEncoder.encode(gameName, StandardCharsets.UTF_8);
        String url = String.format("%s/games?search=%s&key=%s&page_size=1",
                rawgBaseUrl, encodedName, rawgApiKey);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(rawgTimeoutMs))
                .GET()
                .header("User-Agent", "Gamelog/1.0")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("RAWG API returned status: " + response.statusCode());
        }

        return objectMapper.readTree(response.body());
    }

    private JsonNode callRawgGameDetailsBySlug(String slug) throws Exception {
        String encodedSlug = URLEncoder.encode(slug, StandardCharsets.UTF_8);
        String url = String.format("%s/games/%s?key=%s", rawgBaseUrl, encodedSlug, rawgApiKey);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(rawgTimeoutMs))
                .GET()
                .header("User-Agent", "Gamelog/1.0")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("RAWG API returned status: " + response.statusCode());
        }

        return objectMapper.readTree(response.body());
    }

    private JsonNode callRawgGameDetailsById(Long rawgId) throws Exception {
        String url = String.format("%s/games/%d?key=%s", rawgBaseUrl, rawgId, rawgApiKey);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(rawgTimeoutMs))
                .GET()
                .header("User-Agent", "Gamelog/1.0")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("RAWG API returned status: " + response.statusCode());
        }

        return objectMapper.readTree(response.body());
    }

    private Game persistRawgGame(JsonNode gameNode, String fallbackSlug) {
        String rawgSlug = normalizeSlug(gameNode.path("slug").asText(fallbackSlug));
        Game managedGame = gameRepository.findBySlug(rawgSlug).orElseGet(Game::new);

        String name = text(gameNode, "name", managedGame.getName());
        String description = extractDescription(gameNode);
        String descriptionPtBr = translateToTargetLanguage(description, name);
        String developer = firstNestedName(gameNode, "developers");
        String publisher = firstNestedName(gameNode, "publishers");
        String imageUrl = text(gameNode, "background_image", managedGame.getRawgImageUrl());
        LocalDate releaseDate = parseDate(text(gameNode, "released", null));
        Double defaultRating = gameNode.path("rating").isNumber()
            ? convertRawgRatingToTenScale(gameNode.path("rating").asDouble())
            : managedGame.getDefaultRating();

        managedGame.setName(name);
        managedGame.setSlug(StringUtils.hasText(rawgSlug) ? rawgSlug : normalizeSlug(name));
        managedGame.setDescription(description);
        if (StringUtils.hasText(descriptionPtBr)) {
            managedGame.setDescriptionPtBr(descriptionPtBr);
        }
        managedGame.setDeveloper(developer);
        managedGame.setPublisher(publisher);
        managedGame.setReleaseDate(releaseDate);
        managedGame.setRawgImageUrl(imageUrl);
        managedGame.setCover_url(imageUrl);
        managedGame.setImageSource(ImageSource.RAWG);
        managedGame.setImageLastCheckedAt(java.time.OffsetDateTime.now());

        if (managedGame.getAverageRating() == null) {
            managedGame.setAverageRating(0.0);
        }
        if (defaultRating != null) {
            managedGame.setDefaultRating(defaultRating);
        } else if (managedGame.getDefaultRating() == null) {
            managedGame.setDefaultRating(0.0);
        }

        return gameRepository.save(managedGame);
    }

    private String extractDescription(JsonNode node) {
        String descriptionRaw = node.path("description_raw").asText(null);
        if (StringUtils.hasText(descriptionRaw)) {
            return normalizeText(descriptionRaw);
        }

        String htmlDescription = node.path("description").asText(null);
        if (!StringUtils.hasText(htmlDescription)) {
            return null;
        }

        return stripHtml(htmlDescription);
    }

    private String translateToTargetLanguage(String text, String gameName) {
        if (!StringUtils.hasText(text)) {
            return null;
        }

        if (!StringUtils.hasText(translationBaseUrl)) {
            log.debug("Translation skipped for game={} because app.translation.base-url is empty", gameName);
            return null;
        }

        String baseUrl = translationBaseUrl.endsWith("/")
                ? translationBaseUrl.substring(0, translationBaseUrl.length() - 1)
                : translationBaseUrl;
        String requestUrl = baseUrl + "/translate";
        int attempts = Math.max(1, translationRetryCount + 1);

        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                JsonNode requestBody = objectMapper.createObjectNode()
                        .put("q", text)
                        .put("source", translationSourceLang)
                        .put("target", translationTargetLang)
                        .put("format", "text");

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(requestUrl))
                        .header("User-Agent", "Gamelog/1.0")
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofMillis(translationTimeoutMs))
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                        .build();

                HttpClient translationClient = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofMillis(translationConnectTimeoutMs))
                        .build();

                HttpResponse<String> response = translationClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    log.warn(
                            "Translation request failed game={} status={} attempt={}/{}",
                            gameName,
                            response.statusCode(),
                            attempt,
                            attempts
                    );
                    continue;
                }

                JsonNode root = objectMapper.readTree(response.body());
                String translatedText = root.path("translatedText").asText(null);
                return normalizeText(translatedText);
            } catch (Exception ex) {
                log.warn(
                        "Translation request failed game={} attempt={}/{}",
                        gameName,
                        attempt,
                        attempts,
                        ex
                );
            }
        }

        return null;
    }

    private String firstNestedName(JsonNode node, String arrayFieldName) {
        JsonNode array = node.path(arrayFieldName);
        if (!array.isArray() || array.isEmpty()) {
            return null;
        }

        JsonNode first = array.get(0);
        return text(first, "name", null);
    }

    private String text(JsonNode node, String fieldName, String fallback) {
        if (node == null || !node.has(fieldName) || node.get(fieldName).isNull()) {
            return fallback;
        }

        String value = node.get(fieldName).asText(null);
        return StringUtils.hasText(value) ? value : fallback;
    }

    private LocalDate parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        try {
            return LocalDate.parse(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String normalizeSlug(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        String normalized = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        return StringUtils.hasText(normalized) ? normalized : null;
    }

    private String stripHtml(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        String withoutTags = value.replaceAll("<[^>]+>", " ");
        return normalizeText(withoutTags);
    }

    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        String normalized = value.replaceAll("\\s+", " ").trim();
        return StringUtils.hasText(normalized) ? normalized : null;
    }

    private Double convertRawgRatingToTenScale(Double rawgRating) {
        if (rawgRating == null) {
            return null;
        }

        double converted = rawgRating * 2.0;
        return Math.max(0.0, Math.min(10.0, Math.round(converted * 10.0) / 10.0));
    }
}
