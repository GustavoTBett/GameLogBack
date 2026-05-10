package com.gamelog.gamelog.exception.recommendation;

public class RecommendationServiceUnavailableException extends RuntimeException {

    public RecommendationServiceUnavailableException(String message) {
        super(message);
    }

    public RecommendationServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}