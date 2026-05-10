package com.gamelog.gamelog.ai;

import com.gamelog.gamelog.exception.recommendation.RecommendationServiceUnavailableException;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GoogleGenaiClient implements AiClient {

    private final Client client;
    private final String model;

    /**
     * Constructs a GoogleGenaiClient.
     * The official client reads the API key from the environment variable `GEMINI_API_KEY`.
     * If `apiKey` is provided, we attempt to set it into the process environment so the
     * official client can pick it up. For production it's recommended to export GEMINI_API_KEY.
     */
    public GoogleGenaiClient(String apiKey, String model) {
        if (apiKey != null && !apiKey.isBlank()) {
            this.client = Client.builder().apiKey(apiKey).build();
            log.info("Configured google-genai client with provided API key.");
        } else {
            this.client = new Client();
        }
        this.model = normalizeModel(model);
    }

    @Override
    public String generate(String prompt) {
        try {
            GenerateContentResponse response = client.models.generateContent(model, prompt, null);
            return response.text();
        } catch (Exception e) {
            log.error("Error calling google-genai client", e);

            if (isServiceUnavailable(e)) {
                throw new RecommendationServiceUnavailableException(
                        "Serviço de recomendação temporariamente indisponível. Tente novamente em instantes.",
                        e
                );
            }

            throw new RuntimeException("Failed to call Gemini via google-genai", e);
        }
    }

    private boolean isServiceUnavailable(Throwable throwable) {
        Throwable current = throwable;

        while (current != null) {
            String simpleName = current.getClass().getSimpleName();
            String message = current.getMessage();

            if ("ServerException".equals(simpleName)
                    && message != null
                    && (message.contains("503") || message.toLowerCase().contains("service unavailable"))) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }

    private String normalizeModel(String model) {
        if (model == null) {
            return "";
        }

        String normalized = model.trim();
        if (normalized.startsWith("models/")) {
            normalized = normalized.substring("models/".length());
        }

        return normalized;
    }
}
