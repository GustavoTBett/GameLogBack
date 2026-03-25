package com.gamelog.gamelog.exception.rating;

public class AlreadyExistRatingWithUserAndGame extends RuntimeException {
    public AlreadyExistRatingWithUserAndGame(String message) {
        super(message);
    }
}
