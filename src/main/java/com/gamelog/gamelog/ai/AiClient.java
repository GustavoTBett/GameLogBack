package com.gamelog.gamelog.ai;

public interface AiClient {
    /**
     * Generate a text response for the given prompt.
     * Returns the raw assistant text (not parsed JSON) when possible.
     */
    String
    generate(String prompt);
}
