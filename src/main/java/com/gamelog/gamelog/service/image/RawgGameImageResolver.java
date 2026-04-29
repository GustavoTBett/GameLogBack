package com.gamelog.gamelog.service.image;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamelog.gamelog.model.Game;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpTimeoutException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class RawgGameImageResolver implements GameImageResolver {

    private static final Logger log = LoggerFactory.getLogger(RawgGameImageResolver.class);
    private static final String RAWG_USER_AGENT = "Gamelog/1.0";

    private final RawgImagePersistenceService rawgImagePersistenceService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpClient httpClient;
    private final Object rawgRequestMonitor = new Object();

    @Value("${rawg.api.key:}")
    private String rawgApiKey;

    private String effectiveRawgApiKey;

    @Value("${rawg.api.base-url:https://api.rawg.io/api}")
    private String rawgBaseUrl;

    @Value("${rawg.api.timeout-ms:5000}")
    private int rawgTimeoutMs;

    @Value("${rawg.api.min-interval-ms:3500}")
    private long rawgMinIntervalMs;

    @Value("${app.translation.base-url:}")
    private String translationBaseUrl;

    @Value("${app.translation.timeout-ms:5000}")
    private int translationTimeoutMs;

    @Value("${app.translation.connect-timeout-ms:5000}")
    private int translationConnectTimeoutMs;

    @Value("${app.translation.retry-count:1}")
    private int translationRetryCount;

    @Value("${app.translation.source-lang:en}")
    private String translationSourceLang;

    @Value("${app.translation.target-lang:pt}")
    private String translationTargetLang;

    private long nextRawgRequestAtMs;

    @Value("${app.image.vgchartz-base-url:https://www.vgchartz.com}")
    private String vgchartzBaseUrl;

    @Value("${app.image.default-url:https://www.vgchartz.com/games/boxart/default.jpg}")
    private String defaultImageUrl;

    public RawgGameImageResolver(RawgImagePersistenceService rawgImagePersistenceService) {
        this.rawgImagePersistenceService = rawgImagePersistenceService;
    }

    @PostConstruct
    public void initRawgApiKey() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(rawgTimeoutMs))
                .build();

        if (StringUtils.hasText(rawgApiKey)) {
            effectiveRawgApiKey = rawgApiKey.trim();
            return;
        }

        String envRawgKey = System.getenv("RAWG_API");
        if (StringUtils.hasText(envRawgKey)) {
            effectiveRawgApiKey = envRawgKey.trim();
            return;
        }

        String dotenvRawgKey = readRawgApiFromDotEnv();
        if (StringUtils.hasText(dotenvRawgKey)) {
            effectiveRawgApiKey = dotenvRawgKey.trim();
            return;
        }

        effectiveRawgApiKey = "";
    }

    @Override
    public String resolveAndPersistCoverUrl(Game game) {
        if (StringUtils.hasText(game.getRawgImageUrl())) {
            return game.getRawgImageUrl();
        }

        String vgchartzImage = normalizeVgchartzUrl(game.getCover_url());
        if (StringUtils.hasText(vgchartzImage)) {
            return vgchartzImage;
        }

        return defaultImageUrl;
    }

    @Override
    public void enrichRawgMetadataIfMissing(Game game) {
        if (game == null || game.getId() == null) {
            log.debug("RAWG metadata sync skipped because game is null or has no id");
            return;
        }

        boolean needsFirstSync = game.getImageLastCheckedAt() == null;
        boolean missingImage = !StringUtils.hasText(game.getRawgImageUrl());
        boolean missingDescription = !StringUtils.hasText(game.getDescription());
        boolean missingDescriptionPtBr = !StringUtils.hasText(game.getDescriptionPtBr());

        if (!needsFirstSync && !missingImage && !missingDescription && !missingDescriptionPtBr) {
            log.debug(
                    "RAWG metadata sync skipped gameId={} name={} (already synchronized at {})",
                    game.getId(),
                    game.getName(),
                    game.getImageLastCheckedAt()
            );
            return;
        }

        log.info(
            "RAWG metadata sync started gameId={} name={} hasImage={} hasDescription={} hasDescriptionPtBr={} needsFirstSync={}",
                game.getId(),
                game.getName(),
                !missingImage,
                !missingDescription,
            !missingDescriptionPtBr,
                needsFirstSync
        );

        OffsetDateTime now = OffsetDateTime.now();
        RawgMetadata rawgMetadata = fetchRawgMetadata(game);
        if (rawgMetadata == null || (!StringUtils.hasText(rawgMetadata.imageUrl()) && !StringUtils.hasText(rawgMetadata.description()))) {
            log.warn("RAWG metadata sync returned empty data gameId={} name={}", game.getId(), game.getName());
            return;
        }

        log.info(
                "RAWG metadata fetched gameId={} name={} hasImage={} hasDescription={} rawgId={}",
                game.getId(),
                game.getName(),
                StringUtils.hasText(rawgMetadata.imageUrl()),
                StringUtils.hasText(rawgMetadata.description()),
                rawgMetadata.rawgId()
        );

        String descriptionPtBr = null;
        if (StringUtils.hasText(rawgMetadata.description()) && (needsFirstSync || missingDescriptionPtBr)) {
            descriptionPtBr = translateToTargetLanguage(rawgMetadata.description(), game);
        }

        if (!StringUtils.hasText(descriptionPtBr) && StringUtils.hasText(game.getDescriptionPtBr())) {
            descriptionPtBr = game.getDescriptionPtBr();
        }

        rawgImagePersistenceService.persistRawgMetadataIfChanged(
                game.getId(),
                rawgMetadata.imageUrl(),
                rawgMetadata.description(),
            descriptionPtBr,
                now
        );

        if (StringUtils.hasText(rawgMetadata.imageUrl())) {
            game.setRawgImageUrl(rawgMetadata.imageUrl());
        }

        if (StringUtils.hasText(rawgMetadata.description())) {
            game.setDescription(rawgMetadata.description());
        }

        if (StringUtils.hasText(descriptionPtBr)) {
            game.setDescriptionPtBr(descriptionPtBr);
        }

        log.info(
                "RAWG metadata sync finished gameId={} name={} hasImage={} hasDescription={} hasDescriptionPtBr={}",
                game.getId(),
                game.getName(),
                StringUtils.hasText(game.getRawgImageUrl()),
                StringUtils.hasText(game.getDescription()),
                StringUtils.hasText(game.getDescriptionPtBr())
        );
    }

    private String translateToTargetLanguage(String text, Game game) {
        if (!StringUtils.hasText(text)) {
            return null;
        }

        if (!StringUtils.hasText(translationBaseUrl)) {
            log.debug("Translation skipped gameId={} because app.translation.base-url is empty", game.getId());
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
                .header("User-Agent", RAWG_USER_AGENT)
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
                    "Translation request failed gameId={} status={} attempt={}/{}",
                    game.getId(),
                    response.statusCode(),
                    attempt,
                    attempts
                );
                return null;
            }

            JsonNode root = objectMapper.readTree(response.body());
            String translatedText = root.path("translatedText").asText(null);
            String normalized = normalizeText(translatedText);

            log.debug(
                "Translation result gameId={} hasTranslatedText={} attempt={}/{}",
                game.getId(),
                StringUtils.hasText(normalized),
                attempt,
                attempts
            );

            return normalized;
            } catch (HttpTimeoutException ex) {
            log.warn(
                "Translation timeout gameId={} attempt={}/{} timeoutMs={}",
                game.getId(),
                attempt,
                attempts,
                translationTimeoutMs
            );
            } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Translation request interrupted gameId={}", game.getId());
            return null;
            } catch (Exception ex) {
            log.warn(
                "Translation request failed gameId={} attempt={}/{}",
                game.getId(),
                attempt,
                attempts,
                ex
            );
            return null;
            }
        }

        return null;
    }

    public void enrichRawgImageIfMissing(Game game) {
        enrichRawgMetadataIfMissing(game);
    }

    private RawgMetadata fetchRawgMetadata(Game game) {
        if (!StringUtils.hasText(effectiveRawgApiKey) || !StringUtils.hasText(game.getName())) {
            log.warn(
                    "RAWG metadata fetch skipped gameId={} name={} hasApiKey={} hasGameName={}",
                    game.getId(),
                    game.getName(),
                    StringUtils.hasText(effectiveRawgApiKey),
                    StringUtils.hasText(game.getName())
            );
            return null;
        }

        RawgMetadata metadataWithDates = callRawgSearch(game, true);
        RawgMetadata enrichedWithDates = enrichWithDetailIfNeeded(metadataWithDates);
        if (hasUsefulMetadata(enrichedWithDates)) {
            return enrichedWithDates;
        }

        RawgMetadata metadataWithoutDates = callRawgSearch(game, false);
        RawgMetadata enrichedWithoutDates = enrichWithDetailIfNeeded(metadataWithoutDates);

        return mergeMetadata(enrichedWithDates, enrichedWithoutDates);
    }

    private RawgMetadata enrichWithDetailIfNeeded(RawgMetadata metadata) {
        if (metadata == null) {
            return null;
        }

        if (StringUtils.hasText(metadata.description()) || metadata.rawgId() == null) {
            return metadata;
        }

        log.debug("RAWG detail fallback requested rawgId={} because description is missing", metadata.rawgId());

        RawgMetadata detailedMetadata = callRawgGameDetails(metadata.rawgId());
        return mergeMetadata(metadata, detailedMetadata);
    }

    private boolean hasUsefulMetadata(RawgMetadata metadata) {
        return metadata != null
                && (StringUtils.hasText(metadata.imageUrl()) || StringUtils.hasText(metadata.description()));
    }

    private RawgMetadata mergeMetadata(RawgMetadata primary, RawgMetadata fallback) {
        if (primary == null) {
            return fallback;
        }

        if (fallback == null) {
            return primary;
        }

        Long rawgId = primary.rawgId() != null ? primary.rawgId() : fallback.rawgId();
        String imageUrl = StringUtils.hasText(primary.imageUrl()) ? primary.imageUrl() : fallback.imageUrl();
        String description = StringUtils.hasText(primary.description()) ? primary.description() : fallback.description();

        return new RawgMetadata(rawgId, imageUrl, description);
    }

    private RawgMetadata callRawgSearch(Game game, boolean includeDates) {
        try {
            enforceRawgRateLimit();

            String query = URLEncoder.encode(game.getName(), StandardCharsets.UTF_8);
            StringBuilder urlBuilder = new StringBuilder(rawgBaseUrl)
                    .append("/games?search=")
                    .append(query)
                    .append("&page_size=1")
                    .append("&key=")
                    .append(effectiveRawgApiKey);

            if (includeDates && game.getReleaseDate() != null) {
                urlBuilder.append("&dates=")
                        .append(game.getReleaseDate().getYear())
                        .append("-01-01,")
                        .append(game.getReleaseDate().getYear())
                        .append("-12-31");
            }

            String requestUrl = urlBuilder.toString();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(requestUrl))
                    .header("User-Agent", RAWG_USER_AGENT)
                    .timeout(Duration.ofMillis(rawgTimeoutMs))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 429) {
                log.warn("RAWG rate limit reached for gameId={} name={}", game.getId(), game.getName());
                return null;
            }

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("RAWG request failed for gameId={} status={}", game.getId(), response.statusCode());
                return null;
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode results = root.path("results");
            if (!results.isArray() || results.isEmpty()) {
                return null;
            }

            JsonNode firstResult = results.get(0);
            Long rawgId = firstResult.path("id").isNumber() ? firstResult.path("id").asLong() : null;
            String imageUrl = firstResult.path("background_image").asText(null);
            String description = extractDescription(firstResult);

                log.debug(
                    "RAWG search result gameId={} includeDates={} rawgId={} hasImage={} hasDescription={}",
                    game.getId(),
                    includeDates,
                    rawgId,
                    StringUtils.hasText(imageUrl),
                    StringUtils.hasText(description)
                );

            return new RawgMetadata(
                    rawgId,
                    StringUtils.hasText(imageUrl) ? imageUrl : null,
                    StringUtils.hasText(description) ? description : null
            );
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("RAWG lookup interrupted for gameId={} name={}", game.getId(), game.getName(), ex);
            return null;
        } catch (Exception ex) {
            log.warn("RAWG lookup failed for gameId={} name={}", game.getId(), game.getName(), ex);
            return null;
        }
    }

    private RawgMetadata callRawgGameDetails(Long rawgId) {
        try {
            enforceRawgRateLimit();

            String requestUrl = rawgBaseUrl + "/games/" + rawgId + "?key=" + effectiveRawgApiKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(requestUrl))
                    .header("User-Agent", RAWG_USER_AGENT)
                    .timeout(Duration.ofMillis(rawgTimeoutMs))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 429) {
                log.warn("RAWG rate limit reached for rawgId={}", rawgId);
                return null;
            }

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("RAWG detail request failed for rawgId={} status={}", rawgId, response.statusCode());
                return null;
            }

            JsonNode detail = objectMapper.readTree(response.body());
            String imageUrl = detail.path("background_image").asText(null);
            String description = extractDescription(detail);

                log.debug(
                    "RAWG detail result rawgId={} hasImage={} hasDescription={}",
                    rawgId,
                    StringUtils.hasText(imageUrl),
                    StringUtils.hasText(description)
                );

            return new RawgMetadata(
                    rawgId,
                    StringUtils.hasText(imageUrl) ? imageUrl : null,
                    StringUtils.hasText(description) ? description : null
            );
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("RAWG detail lookup interrupted for rawgId={}", rawgId, ex);
            return null;
        } catch (Exception ex) {
            log.warn("RAWG detail lookup failed for rawgId={}", rawgId, ex);
            return null;
        }
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

    private record RawgMetadata(Long rawgId, String imageUrl, String description) {
    }

    private void enforceRawgRateLimit() throws InterruptedException {
        synchronized (rawgRequestMonitor) {
            long now = System.currentTimeMillis();
            long waitTimeMs = nextRawgRequestAtMs - now;
            if (waitTimeMs > 0) {
                Thread.sleep(waitTimeMs);
            }

            nextRawgRequestAtMs = System.currentTimeMillis() + rawgMinIntervalMs;
        }
    }

    private String readRawgApiFromDotEnv() {
        List<Path> candidates = List.of(
                Path.of(".env"),
                Path.of("gamelogback", ".env")
        );

        for (Path candidate : candidates) {
            if (!Files.exists(candidate)) {
                continue;
            }

            try {
                List<String> lines = Files.readAllLines(candidate, StandardCharsets.UTF_8);
                for (String rawLine : lines) {
                    String line = rawLine.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }

                    if (line.startsWith("export ")) {
                        line = line.substring("export ".length()).trim();
                    }

                    if (!line.startsWith("RAWG_API=")) {
                        continue;
                    }

                    String value = line.substring("RAWG_API=".length()).trim();
                    if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }

                    if (StringUtils.hasText(value)) {
                        return value;
                    }
                }
            } catch (IOException ex) {
                // Ignore and continue with next candidate.
            }
        }

        return null;
    }

    private String normalizeVgchartzUrl(String coverUrl) {
        if (!StringUtils.hasText(coverUrl)) {
            return null;
        }

        if (coverUrl.startsWith("http://") || coverUrl.startsWith("https://")) {
            return coverUrl;
        }

        String normalizedBase = vgchartzBaseUrl.endsWith("/")
                ? vgchartzBaseUrl.substring(0, vgchartzBaseUrl.length() - 1)
                : vgchartzBaseUrl;
        String normalizedPath = coverUrl.startsWith("/") ? coverUrl : "/" + coverUrl;

        return normalizedBase + normalizedPath;
    }
}
