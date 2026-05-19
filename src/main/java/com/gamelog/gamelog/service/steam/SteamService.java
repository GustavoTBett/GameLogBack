package com.gamelog.gamelog.service.steam;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.HtmlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@Service
public class SteamService {

    private static final Logger log = LoggerFactory.getLogger(SteamService.class);

    private static final Pattern RECOMMENDED_PATH_PATTERN = Pattern.compile("/recommended/(\\d+)/");
    private static final Pattern APP_NAME_PATTERN = Pattern.compile("(?is)<div[^>]*class=[\"'][^\"']*\\bapphub_AppName\\b[^\"']*[\"'][^>]*>(.*?)</div>");
    private static final Pattern REVIEW_TEXT_PATTERN = Pattern.compile("(?is)<div[^>]*id=\"ReviewText\"[^>]*>(.*?)</div>");
    private static final Pattern REVIEW_TITLE_BLOCK_PATTERN = Pattern.compile("(?is)<div[^>]*class=[\"'][^\"']*\\btitle\\b[^\"']*[\"'][^>]*>(.*?)</div>");
    private static final Pattern REVIEW_DATE_PATTERN = Pattern.compile("(?is)<div[^>]*class=[\"'][^\"']*\\bdate_posted\\b[^\"']*[\"'][^>]*>(.*?)</div>");
    private static final Pattern REVIEW_TEXT_START_PATTERN = Pattern.compile("(?is)<div[^>]*id=[\"']ReviewText[\"'][^>]*>");
    private static final DateTimeFormatter REVIEW_TIMESTAMP_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("MMM d, uuuu @ h:mma")
            .toFormatter(Locale.ENGLISH);

    private final RestTemplate restTemplate;

    private final String apiKey;

    public SteamService(RestTemplate restTemplate, @Value("${app.steam.api-key:}") String apiKey) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        log.debug("SteamService initialized (apiKey present={})", apiKey != null && !apiKey.isBlank());
    }

    public Map<String, Object> getPlayerSummary(String steamId) {
        log.debug("getPlayerSummary: steamId={}", steamId);
        String url = String.format(
                "https://api.steampowered.com/ISteamUser/GetPlayerSummaries/v2/?key=%s&steamids=%s",
                apiKey, steamId
        );

        try {
            ResponseEntity<Map> resp = restTemplate.getForEntity(url, Map.class);
            Map body = resp.getBody();
            if (body == null) return Map.of();
            Map response = (Map) body.get("response");
            if (response == null) return Map.of();
            List players = (List) response.get("players");
            if (players != null && !players.isEmpty()) {
                return (Map<String, Object>) players.get(0);
            }
            return Map.of();
        } catch (RestClientException ex) {
            log.warn("Failed to fetch player summary for {}: {}", steamId, ex.getMessage());
            throw new RuntimeException("Failed to fetch player summary", ex);
        }
    }

    public List<Map<String, Object>> getUserReviews(String steamId) {
        log.debug("getUserReviews: steamId={}", steamId);
        try {
            Set<Long> appIds = new LinkedHashSet<>();
            int page = 1;

            while (page <= 50) {
                log.debug("Fetching recommended listing page {} for {}", page, steamId);
                String listingHtml = fetchSteamCommunityPage(String.format(
                        "https://steamcommunity.com/profiles/%s/recommended/?p=%d&l=english",
                        steamId,
                        page
                ));

                Set<Long> pageAppIds = extractRecommendedAppIds(listingHtml);
                log.debug("Found {} appIds on page {} for {}", pageAppIds.size(), page, steamId);
                if (pageAppIds.isEmpty()) {
                    break;
                }

                boolean addedAny = appIds.addAll(pageAppIds);
                if (!addedAny) {
                    break;
                }

                page++;
            }

            log.info("Total recommended appIds found for {}: {}", steamId, appIds.size());
            return appIds.stream()
                    .map(appId -> fetchReviewDetails(steamId, appId))
                    .filter(review -> !review.isEmpty())
                    .toList();
        } catch (RestClientException ex) {
            log.warn("Failed to fetch user reviews for {}: {}", steamId, ex.getMessage());
            throw new RuntimeException("Failed to fetch user reviews", ex);
        }
    }

    public String getAppName(Long appId) {
        if (appId == null) {
            return null;
        }
        log.debug("getAppName: appId={}", appId);

        String url = String.format(
                "https://store.steampowered.com/api/appdetails?appids=%d&l=english",
                appId
        );

        try {
            ResponseEntity<Map> resp = restTemplate.getForEntity(url, Map.class);
            Map body = resp.getBody();
            if (body == null) {
                return null;
            }

            Object entry = body.get(String.valueOf(appId));
            if (!(entry instanceof Map appDetails)) {
                return null;
            }

            Object success = appDetails.get("success");
            if (!(success instanceof Boolean successValue) || !successValue) {
                return null;
            }

            Object data = appDetails.get("data");
            if (!(data instanceof Map dataMap)) {
                return null;
            }

            Object name = dataMap.get("name");
            return name == null ? null : String.valueOf(name).trim();
        } catch (RestClientException ex) {
            log.warn("Failed to fetch app details for {}: {}", appId, ex.getMessage());
            throw new RuntimeException("Failed to fetch app details", ex);
        }
    }

    public String resolveVanityUrl(String vanityUrl) {
        log.debug("resolveVanityUrl: vanityUrl={}", vanityUrl);
        String url = String.format(
                "https://api.steampowered.com/ISteamUser/ResolveVanityURL/v1/?key=%s&vanityurl=%s",
                apiKey,
                vanityUrl
        );

        try {
            ResponseEntity<Map> resp = restTemplate.getForEntity(url, Map.class);
            Map body = resp.getBody();
            if (body == null) {
                return null;
            }

            Map response = (Map) body.get("response");
            if (response == null) {
                return null;
            }

            Number success = (Number) response.get("success");
            if (success == null || success.intValue() != 1) {
                return null;
            }

            Object steamId = response.get("steamid");
            return steamId == null ? null : String.valueOf(steamId);
        } catch (RestClientException ex) {
            log.warn("Failed to resolve vanity URL {}: {}", vanityUrl, ex.getMessage());
            throw new RuntimeException("Failed to resolve Steam vanity URL", ex);
        }
    }

    private Map<String, Object> fetchReviewDetails(String steamId, Long appId) {
        log.debug("fetchReviewDetails: steamId={}, appId={}", steamId, appId);
        String html = fetchSteamCommunityPage(String.format(
                "https://steamcommunity.com/profiles/%s/recommended/%d/?l=english",
                steamId,
                appId
        ));

        String appName = extractFirstGroup(html, APP_NAME_PATTERN);
        String reviewText = extractReviewText(html);
        Boolean votedUp = extractRecommendation(html);
        Long timestampCreated = extractPostedTimestamp(html);

        // Fallback: if we couldn't determine recommendation from profile page, try Store appreviews API
        if (votedUp == null) {
            try {
                Map<String, Object> storeReview = fetchUserReviewFromStoreApi(steamId, appId);
                if (storeReview != null) {
                    Object vu = storeReview.get("voted_up");
                    if (vu instanceof Boolean) {
                        votedUp = (Boolean) vu;
                    }
                    if ((reviewText == null || reviewText.isBlank()) && storeReview.get("review") != null) {
                        reviewText = String.valueOf(storeReview.get("review"));
                    }
                    if (timestampCreated == null && storeReview.get("timestamp_created") instanceof Number) {
                        timestampCreated = ((Number) storeReview.get("timestamp_created")).longValue();
                    }
                    log.info("fetchReviewDetails: fallback to store API for steamId={}, appId={}, voted_up={}", steamId, appId, votedUp);
                } else {
                    log.debug("fetchReviewDetails: store API did not return a review for steamId={}, appId={}", steamId, appId);
                }
            } catch (Exception ex) {
                log.warn("fetchReviewDetails: error querying store API for steamId={}, appId={}: {}", steamId, appId, ex.getMessage());
            }
        }

        if (reviewText == null && appName == null && votedUp == null && timestampCreated == null) {
            log.debug("No review details found for steamId={}, appId={}", steamId, appId);
            return Map.of();
        }

        Map<String, Object> review = new LinkedHashMap<>();
        review.put("appid", appId);
        review.put("app_name", appName);
        review.put("review", reviewText);
        if (votedUp != null) {
            review.put("voted_up", votedUp);
        }
        if (timestampCreated != null) {
            review.put("timestamp_created", timestampCreated);
        }
        review.put("language", "english");
        String preview = reviewText == null ? null : (reviewText.length() > 200 ? reviewText.substring(0, 200) + "..." : reviewText);
        log.info("Parsed Steam review: steamId={}, appId={}, appName={}, recommended={}, timestamp={}, preview={}",
            steamId, appId, appName, votedUp, timestampCreated, preview);
        log.debug("Built review for steamId={}, appId={}, keys={}", steamId, appId, review.keySet());
        return review;
    }

    private String fetchSteamCommunityPage(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, MediaType.TEXT_HTML_VALUE);
        headers.set(HttpHeaders.USER_AGENT, "Gamelog/1.0");

        log.debug("Fetching Steam community page: {}", url);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.GET,
                    new HttpEntity<Void>(headers),
                    String.class
            );

            String body = response.getBody();
            if (body == null) {
                body = "";
            }
            log.debug("Fetched Steam page {} (length={})", url, body.length());
            if (!body.isBlank()) {
                String snippet = body.length() > 1000 ? body.substring(0, 1000) + "..." : body;
                log.debug("Response snippet for {}: {}", url, snippet);
            }
            return body;
        } catch (RestClientException ex) {
            log.warn("Error fetching Steam community page {}: {}", url, ex.getMessage());
            throw ex;
        }
    }

    private Map<String, Object> fetchUserReviewFromStoreApi(String steamId, Long appId) {
        String base = String.format("https://store.steampowered.com/appreviews/%d?json=1&language=all&filter=all&num_per_page=100", appId);
        String cursor = "*";
        int attempts = 0;
        while (attempts < 10) { // avoid infinite loop
            attempts++;
            String pageUrl = base + "&cursor=" + URLEncoder.encode(cursor, StandardCharsets.UTF_8);
            log.debug("fetchUserReviewFromStoreApi: querying {} for steamId={}", pageUrl, steamId);
            try {
                ResponseEntity<Map> resp = restTemplate.getForEntity(pageUrl, Map.class);
                Map body = resp.getBody();
                if (body == null) break;
                Object reviewsObj = body.get("reviews");
                if (reviewsObj instanceof List reviews) {
                    for (Object ro : reviews) {
                        if (!(ro instanceof Map)) continue;
                        Map review = (Map) ro;
                        Object authorObj = review.get("author");
                        if (!(authorObj instanceof Map)) continue;
                        Map author = (Map) authorObj;
                        Object aid = author.get("steamid");
                        if (aid != null && String.valueOf(aid).equals(steamId)) {
                            log.debug("fetchUserReviewFromStoreApi: found review for steamId={} in appId={}", steamId, appId);
                            return review;
                        }
                    }
                }
                Object cursorObj = body.get("cursor");
                if (cursorObj == null) break;
                cursor = String.valueOf(cursorObj);
                if (cursor == null || cursor.isBlank()) break;
            } catch (RestClientException ex) {
                log.warn("fetchUserReviewFromStoreApi: request failed for {}: {}", appId, ex.getMessage());
                break;
            }
        }
        return null;
    }

    private static Set<Long> extractRecommendedAppIds(String html) {
        Set<Long> appIds = new LinkedHashSet<>();
        Matcher matcher = RECOMMENDED_PATH_PATTERN.matcher(html);
        while (matcher.find()) {
            appIds.add(Long.valueOf(matcher.group(1)));
        }
        return appIds;
    }

    private static String extractFirstGroup(String html, Pattern pattern) {
        String value = extractFirstRawGroup(html, pattern);
        return cleanHtmlFragment(value);
    }

    private static String extractFirstRawGroup(String html, Pattern pattern) {
        Matcher matcher = pattern.matcher(html);
        if (!matcher.find()) {
            return null;
        }

        String group = matcher.group(1);
        if (group == null) {
            log.debug("extractFirstRawGroup: pattern={} matched but group is null", pattern.pattern());
            return null;
        }
        String snippet = group.length() > 500 ? group.substring(0, 500) + "..." : group;
        log.debug("extractFirstRawGroup: pattern={} found (length={}) snippet={}", pattern.pattern(), group.length(), snippet);
        return group;
    }

    private static String extractReviewText(String html) {
        Matcher matcher = REVIEW_TEXT_PATTERN.matcher(html);
        if (!matcher.find()) {
            return null;
        }

        String raw = matcher.group(1);
        String text = cleanHtmlFragment(raw);
        if (text == null || text.isBlank()) {
            log.debug("extractReviewText: found review block but cleaned text is empty");
            return null;
        }
        String preview = text.length() > 300 ? text.substring(0, 300) + "..." : text;
        log.debug("extractReviewText: extracted review length={} preview={}", text.length(), preview);
        return text;
    }

    private static Boolean extractRecommendation(String html) {
        String titleHtml = extractFirstRawGroup(html, REVIEW_TITLE_BLOCK_PATTERN);
        Boolean fromTitle = extractRecommendationText(cleanTitleFragment(titleHtml));
        if (fromTitle != null) {
            log.debug("extractRecommendation: result={} from title block", fromTitle);
            return fromTitle;
        }

        Boolean fromKnownMarkers = extractRecommendationFromKnownMarkers(html);
        if (fromKnownMarkers != null) {
            log.debug("extractRecommendation: result={} from known markers", fromKnownMarkers);
            return fromKnownMarkers;
        }

        Boolean fromPreReview = extractRecommendationText(cleanTitleFragment(htmlBeforeReviewText(html)));
        if (fromPreReview != null) {
            log.debug("extractRecommendation: result={} from pre-review text", fromPreReview);
            return fromPreReview;
        }

        log.debug("extractRecommendation: no recommendation marker found");
        return null;
    }

    private static Boolean extractRecommendationText(String titleText) {
        if (titleText == null) {
            return null;
        }

        String normalized = titleText.toLowerCase(Locale.ENGLISH);
        String snippet = titleText.length() > 200 ? titleText.substring(0, 200) + "..." : titleText;
        log.debug("extractRecommendation: titleText='{}' normalized='{}'", snippet, normalized);

        Boolean result;
        if (normalized.contains("not recommended")) {
            result = false;
        } else if (normalized.contains("recommended")) {
            result = true;
        } else {
            result = null;
        }
        log.debug("extractRecommendation: result={}", result);
        return result;
    }

    private static Boolean extractRecommendationFromKnownMarkers(String html) {
        if (html == null || html.isBlank()) {
            return null;
        }

        String normalized = html.toLowerCase(Locale.ENGLISH);
        if (normalized.contains("review_summary not_recommended")
                || normalized.contains("review_summary negative")
                || normalized.contains("recommendationblock negative")
                || normalized.contains("iconthumbsdown")) {
            return false;
        }

        if (normalized.contains("review_summary recommended")
                || normalized.contains("review_summary positive")
                || normalized.contains("recommendationblock positive")
                || normalized.contains("iconthumbsup")) {
            return true;
        }

        return null;
    }

    private static String htmlBeforeReviewText(String html) {
        if (html == null) {
            return null;
        }

        Matcher matcher = REVIEW_TEXT_START_PATTERN.matcher(html);
        if (!matcher.find()) {
            return html;
        }

        return html.substring(0, matcher.start());
    }

    private static Long extractPostedTimestamp(String html) {
        Matcher matcher = REVIEW_DATE_PATTERN.matcher(html);
        if (!matcher.find()) {
            return null;
        }

        String text = cleanHtmlFragment(matcher.group(1));
        if (text == null) {
            return null;
        }

        for (String line : text.split("\\R")) {
            String normalized = line.trim();
            if (!normalized.startsWith("Posted:")) {
                continue;
            }

            String rawTimestamp = normalized.substring("Posted:".length()).trim();
            log.debug("extractPostedTimestamp: rawTimestamp='{}'", rawTimestamp);
            try {
                LocalDateTime parsed = LocalDateTime.parse(rawTimestamp, REVIEW_TIMESTAMP_FORMATTER);
                long epoch = parsed.toInstant(ZoneOffset.UTC).getEpochSecond();
                log.debug("extractPostedTimestamp: parsed epoch={}", epoch);
                return epoch;
            } catch (DateTimeParseException ignored) {
                log.debug("extractPostedTimestamp: failed to parse timestamp='{}'", rawTimestamp);
                return null;
            }
        }

        return null;
    }

    private static String cleanHtmlFragment(String html) {
        if (html == null) {
            return null;
        }

        String normalized = html
                .replaceAll("(?is)<br\\s*/?>", "\n")
                .replaceAll("(?is)</p\\s*>", "\n")
                .replaceAll("(?is)</div\\s*>", "\n")
                .replaceAll("(?is)<[^>]+>", "");

        normalized = HtmlUtils.htmlUnescape(normalized)
                .replace('\u00A0', ' ')
                .replace("\r", "");

        normalized = normalized.replaceAll("[\\t\\x0B\\f ]*\\n[\\t\\x0B\\f ]*", "\n");
        normalized = normalized.replaceAll("\\n{3,}", "\n\n").trim();

        return normalized.isBlank() ? null : normalized;
    }

    private static String cleanTitleFragment(String html) {
        if (html == null) {
            return null;
        }

        String normalized = HtmlUtils.htmlUnescape(html.replaceAll("(?is)<[^>]+>", " "))
                .replace('\u00A0', ' ')
                .replace("\r", " ")
                .replace("\n", " ")
                .replaceAll("\\s+", " ")
                .trim();

        return normalized.isBlank() ? null : normalized;
    }
}
