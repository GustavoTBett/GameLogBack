package com.gamelog.gamelog.service.steam;

import com.gamelog.gamelog.model.*;
import com.gamelog.gamelog.repository.GameRepository;
import com.gamelog.gamelog.repository.SteamAccountRepository;
import com.gamelog.gamelog.repository.SteamUserReviewRepository;
import com.gamelog.gamelog.service.translation.TranslationService;
import com.gamelog.gamelog.service.recommendation.RawgGameImportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Objects;

@Service
@Slf4j
public class SteamSyncService {

    private static final Pattern STEAM_ID64_PATTERN = Pattern.compile("^\\d{17}$");
    private static final Pattern STEAM_PROFILE_URL_PATTERN = Pattern.compile("^https?://steamcommunity\\.com/profiles/(\\d{17})(?:/.*)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern STEAM_VANITY_URL_PATTERN = Pattern.compile("^https?://steamcommunity\\.com/id/([^/?#]+)(?:/.*)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern VANITY_IDENTIFIER_PATTERN = Pattern.compile("^[A-Za-z0-9_\\-]+$");

    private final SteamService steamService;
    private final SteamAccountRepository steamAccountRepository;
    private final RawgGameImportService rawgGameImportService;
    private final GameRepository gameRepository;
    private final SteamUserReviewRepository steamUserReviewRepository;
    private final TranslationService translationService;

    public SteamSyncService(
            SteamService steamService,
            SteamAccountRepository steamAccountRepository,
            RawgGameImportService rawgGameImportService,
            GameRepository gameRepository,
            SteamUserReviewRepository steamUserReviewRepository,
            TranslationService translationService
    ) {
        this.steamService = steamService;
        this.steamAccountRepository = steamAccountRepository;
        this.rawgGameImportService = rawgGameImportService;
        this.gameRepository = gameRepository;
        this.steamUserReviewRepository = steamUserReviewRepository;
        this.translationService = translationService;
    }

    @Transactional
    public ImportSummary importUserReviews(SteamAccount account, String profileIdentifier) {
        String steamId = resolveSteamId(profileIdentifier);

        Map<String, Object> summary = steamService.getPlayerSummary(steamId);
        if (summary == null || summary.isEmpty()) {
            throw new IllegalStateException("Perfil Steam não encontrado.");
        }

        Number visibility = (Number) summary.get("communityvisibilitystate");
        if (visibility == null || visibility.intValue() != 3) {
            throw new IllegalStateException("Nenhuma avaliação pública encontrada para este perfil Steam.");
        }

        List<Map<String, Object>> reviews;
        try {
            reviews = steamService.getUserReviews(steamId);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Não foi possível buscar as avaliações deste perfil Steam.", ex);
        }

        List<SteamUserReview> existingReviews = steamUserReviewRepository.findAllBySteamAccount(account);
        Map<Long, SteamUserReview> existingByAppId = new HashMap<>();
        for (SteamUserReview existingReview : existingReviews) {
            if (existingReview.getAppId() != null) {
                existingByAppId.put(existingReview.getAppId(), existingReview);
            }
        }

        if (reviews == null || reviews.isEmpty()) {
            for (SteamUserReview existingReview : existingReviews) {
                if (Boolean.TRUE.equals(existingReview.getActive())) {
                    existingReview.setActive(false);
                    steamUserReviewRepository.save(existingReview);
                }
            }

            throw new IllegalStateException("Nenhuma avaliação pública encontrada para este perfil Steam.");
        }

        Set<Long> seenAppIds = new HashSet<>();
        int importedGames = 0;
        int savedReviews = 0;
        boolean foundValidReview = false;
        Instant now = Instant.now();

        for (Map<String, Object> rawReview : reviews) {
            ParsedSteamUserReview review = parseSteamUserReview(rawReview);
            if (review == null) {
                continue;
            }

            foundValidReview = true;
            seenAppIds.add(review.appId());

            Game matched = resolveGame(review.appId(), review.appName());
            if (StringUtils.hasText(review.appName()) && matched == null) {
                try {
                    matched = rawgGameImportService.importGameByName(review.appName());
                    importedGames++;
                } catch (Exception ex) {
                    log.debug("RAWG import failed for '{}': {}", review.appName(), ex.getMessage());
                }
            }

            if (matched == null) {
                matched = gameRepository.findBySteamAppId(review.appId()).orElse(null);
            }

            if (matched == null) {
                String fallbackAppName = null;
                try {
                    fallbackAppName = steamService.getAppName(review.appId());
                } catch (Exception ex) {
                    log.debug("Steam app details lookup failed for appId={}: {}", review.appId(), ex.getMessage());
                }

                if (StringUtils.hasText(fallbackAppName)) {
                    matched = resolveGame(review.appId(), fallbackAppName);
                }

                if (StringUtils.hasText(fallbackAppName) && matched == null) {
                    try {
                        matched = rawgGameImportService.importGameByName(fallbackAppName);
                        importedGames++;
                    } catch (Exception ex) {
                        log.debug("RAWG import failed for Steam app '{}' (appId={}): {}", fallbackAppName, review.appId(), ex.getMessage());
                    }
                }

                if (matched == null) {
                    matched = gameRepository.findBySteamAppId(review.appId()).orElse(null);
                }
            }

            String translatedText = translateReviewText(review.reviewText());
            SteamUserReview existing = existingByAppId.get(review.appId());

            if (existing == null) {
                SteamUserReview created = SteamUserReview.builder()
                        .steamAccount(account)
                        .appId(review.appId())
                        .build();
                applyUserReview(created, matched, translatedText, review.language(), review.recommended(), review.reviewedAt(), true, now);
                steamUserReviewRepository.save(created);
                savedReviews++;
                continue;
            }

            boolean changed = applyUserReview(existing, matched, translatedText, review.language(), review.recommended(), review.reviewedAt(), true, now);
            if (changed) {
                steamUserReviewRepository.save(existing);
                savedReviews++;
            }
        }

        if (!foundValidReview) {
            throw new IllegalStateException("Resposta incompleta da Steam para este perfil Steam.");
        }

        for (SteamUserReview existingReview : existingReviews) {
            Long appId = existingReview.getAppId();
            if (appId != null && !seenAppIds.contains(appId) && Boolean.TRUE.equals(existingReview.getActive())) {
                existingReview.setActive(false);
                steamUserReviewRepository.save(existingReview);
            }
        }

        account.setLastSyncedAt(now);
        account.setSynced(true);
        steamAccountRepository.save(account);

        return new ImportSummary(reviews.size(), importedGames, savedReviews);
    }

    private Game resolveGame(Long appId, String appName) {
        if (appId != null) {
            Game bySteamAppId = gameRepository.findBySteamAppId(appId).orElse(null);
            if (bySteamAppId != null) {
                return bySteamAppId;
            }
        }

        if (!StringUtils.hasText(appName)) {
            return null;
        }

        return gameRepository.findByName(appName).orElse(null);
    }

    private String translateReviewText(String reviewText) {
        if (!StringUtils.hasText(reviewText) || translationService == null) {
            return reviewText;
        }

        try {
            String translated = translationService.translate(reviewText, "pt");
            return StringUtils.hasText(translated) ? translated : reviewText;
        } catch (Exception ex) {
            return reviewText;
        }
    }

    private ParsedSteamUserReview parseSteamUserReview(Map<String, Object> rawReview) {
        if (rawReview == null || rawReview.isEmpty()) {
            return null;
        }

        Number appIdNumber = asNumber(rawReview.get("appid"));
        if (appIdNumber == null) {
            return null;
        }

        Long appId = appIdNumber.longValue();
        String appName = asString(rawReview.get("app_name"));
        String reviewText = asString(rawReview.get("review"));
        String language = asString(rawReview.get("language"));
        Boolean recommended = asBoolean(rawReview.get("voted_up"));
        if (recommended == null) {
            recommended = asBoolean(rawReview.get("recommended"));
        }
        Instant reviewedAt = asInstant(rawReview.get("timestamp_created"));
        if (reviewedAt == null) {
            reviewedAt = asInstant(rawReview.get("timestamp_updated"));
        }

        return new ParsedSteamUserReview(appId, appName, reviewText, language, recommended, reviewedAt);
    }

    private static boolean applyUserReview(
            SteamUserReview review,
            Game game,
            String reviewText,
            String language,
            Boolean recommended,
            Instant reviewedAt,
            boolean active,
            Instant importedAt
    ) {
        boolean changed = false;

        if (!sameGame(review.getGame(), game)) {
            review.setGame(game);
            changed = true;
        }

        if (!Objects.equals(review.getReviewText(), reviewText)) {
            review.setReviewText(reviewText);
            changed = true;
        }

        if (!Objects.equals(review.getLanguage(), language)) {
            review.setLanguage(language);
            changed = true;
        }

        if (!Objects.equals(review.getRecommended(), recommended)) {
            review.setRecommended(recommended);
            changed = true;
        }

        if (!Objects.equals(review.getReviewedAt(), reviewedAt)) {
            review.setReviewedAt(reviewedAt);
            changed = true;
        }

        if (!Objects.equals(review.getActive(), active)) {
            review.setActive(active);
            changed = true;
        }

        if (changed) {
            review.setImportedAt(importedAt);
        }

        return changed;
    }

    private static boolean sameGame(Game left, Game right) {
        if (left == right) {
            return true;
        }

        if (left == null || right == null) {
            return false;
        }

        if (left.getId() != null && right.getId() != null) {
            return Objects.equals(left.getId(), right.getId());
        }

        if (StringUtils.hasText(left.getSlug()) && StringUtils.hasText(right.getSlug())) {
            return left.getSlug().equalsIgnoreCase(right.getSlug());
        }

        return StringUtils.hasText(left.getName())
                && StringUtils.hasText(right.getName())
                && left.getName().equalsIgnoreCase(right.getName());
    }

    private static Number asNumber(Object value) {
        return value instanceof Number number ? number : null;
    }

    private static String asString(Object value) {
        return value instanceof String stringValue && StringUtils.hasText(stringValue) ? stringValue.trim() : null;
    }

    private static Boolean asBoolean(Object value) {
        return value instanceof Boolean booleanValue ? booleanValue : null;
    }

    private static Instant asInstant(Object value) {
        Number number = asNumber(value);
        if (number == null) {
            return null;
        }

        long epochSeconds = number.longValue();
        return Instant.ofEpochSecond(epochSeconds);
    }

    private record ParsedSteamUserReview(
            Long appId,
            String appName,
            String reviewText,
            String language,
            Boolean recommended,
            Instant reviewedAt
    ) {
    }

    private String resolveSteamId(String profileIdentifier) {
        if (!StringUtils.hasText(profileIdentifier)) {
            throw new IllegalArgumentException("Perfil Steam é obrigatório.");
        }

        String candidate = profileIdentifier.trim();

        Matcher profileUrl = STEAM_PROFILE_URL_PATTERN.matcher(candidate);
        if (profileUrl.matches()) {
            return profileUrl.group(1);
        }

        Matcher vanityUrl = STEAM_VANITY_URL_PATTERN.matcher(candidate);
        if (vanityUrl.matches()) {
            String resolved = steamService.resolveVanityUrl(vanityUrl.group(1));
            if (StringUtils.hasText(resolved)) {
                return resolved;
            }
            throw new IllegalStateException("Perfil Steam não encontrado.");
        }

        if (STEAM_ID64_PATTERN.matcher(candidate).matches()) {
            return candidate;
        }

        if (VANITY_IDENTIFIER_PATTERN.matcher(candidate).matches()) {
            String resolved = steamService.resolveVanityUrl(candidate);
            if (StringUtils.hasText(resolved)) {
                return resolved;
            }
            throw new IllegalStateException("Perfil Steam não encontrado.");
        }

        throw new IllegalArgumentException("Perfil Steam inválido.");
    }

    public record ImportSummary(int totalReviews, int importedGames, int savedReviews) {}
}

