package com.gamelog.gamelog.exception.favorite;

public class AlreadyExistFavoriteWithUserAndGame extends RuntimeException {
    public AlreadyExistFavoriteWithUserAndGame(String message) {
        super(message);
    }
}
