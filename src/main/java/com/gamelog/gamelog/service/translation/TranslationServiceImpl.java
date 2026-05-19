package com.gamelog.gamelog.service.translation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class TranslationServiceImpl implements TranslationService {

    private final ObjectMapper objectMapper;
    @Value("${app.translation.base-url:}")
    private String translationBaseUrl;

    @Value("${app.translation.timeout-ms:15000}")
    private int translationTimeoutMs;

    @Value("${app.translation.connect-timeout-ms:5000}")
    private int translationConnectTimeoutMs;

    @Value("${app.translation.source-lang:en}")
    private String translationSourceLang;

    public TranslationServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String translate(String text, String targetLang) {
        if (!StringUtils.hasText(text)) {
            return null;
        }

        if (!StringUtils.hasText(translationBaseUrl) || !StringUtils.hasText(targetLang)) {
            return text;
        }

        String baseUrl = translationBaseUrl.endsWith("/")
                ? translationBaseUrl.substring(0, translationBaseUrl.length() - 1)
                : translationBaseUrl;

        try {
            JsonNode requestBody = objectMapper.createObjectNode()
                    .put("q", text)
                    .put("source", translationSourceLang)
                    .put("target", targetLang)
                    .put("format", "text");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/translate"))
                    .header("User-Agent", "Gamelog/1.0")
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofMillis(translationTimeoutMs))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody), StandardCharsets.UTF_8))
                    .build();

                HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(translationConnectTimeoutMs))
                    .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return text;
            }

            JsonNode root = objectMapper.readTree(response.body());
            String translatedText = root.path("translatedText").asText(null);
            return StringUtils.hasText(translatedText) ? translatedText.trim() : text;
        } catch (Exception ex) {
            return text;
        }
    }
}
