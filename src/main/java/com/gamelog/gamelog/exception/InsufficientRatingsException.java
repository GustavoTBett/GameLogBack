package com.gamelog.gamelog.exception;

public class InsufficientRatingsException extends RuntimeException {
    
    public InsufficientRatingsException(String message) {
        super(message);
    }

    public InsufficientRatingsException(String message, Throwable cause) {
        super(message, cause);
    }
}
