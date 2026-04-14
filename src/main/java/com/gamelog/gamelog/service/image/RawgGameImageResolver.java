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
    private final HttpClient httpClient;
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

    private long nextRawgRequestAtMs;

    @Value("${app.image.vgchartz-base-url:https://www.vgchartz.com}")
    private String vgchartzBaseUrl;

    @Value("${app.image.default-url:https://www.vgchartz.com/games/boxart/default.jpg}")
    private String defaultImageUrl;

    public RawgGameImageResolver(RawgImagePersistenceService rawgImagePersistenceService) {
        this.rawgImagePersistenceService = rawgImagePersistenceService;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(5000))
                .build();
    }

    @PostConstruct
    public void initRawgApiKey() {
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

    public void enrichRawgImageIfMissing(Game game) {
        if (game == null || game.getId() == null || StringUtils.hasText(game.getRawgImageUrl())) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        String rawgImage = fetchRawgImage(game);
        if (StringUtils.hasText(rawgImage)) {
            rawgImagePersistenceService.persistRawgImageIfChanged(game.getId(), rawgImage, now);
        }
    }

    private String fetchRawgImage(Game game) {
        if (!StringUtils.hasText(effectiveRawgApiKey) || !StringUtils.hasText(game.getName())) {
            return null;
        }

        String rawgImage = callRawg(game, true);
        if (StringUtils.hasText(rawgImage)) {
            return rawgImage;
        }

        return callRawg(game, false);
    }

    private String callRawg(Game game, boolean includeDates) {
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

            String imageUrl = results.get(0).path("background_image").asText(null);
            return StringUtils.hasText(imageUrl) ? imageUrl : null;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("RAWG lookup interrupted for gameId={} name={}", game.getId(), game.getName(), ex);
            return null;
        } catch (Exception ex) {
            log.warn("RAWG lookup failed for gameId={} name={}", game.getId(), game.getName(), ex);
            return null;
        }
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
