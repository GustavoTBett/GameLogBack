package com.gamelog.gamelog.service.translation;

public interface TranslationService {

    /**
     * Translate text to target language (e.g., pt).
     * Returns original text if translation not available or fails.
     */
    String translate(String text, String targetLang);
}
