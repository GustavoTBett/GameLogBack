package com.gamelog.gamelog.exception.genre;

public class AlreadyExistGenreWithName extends RuntimeException {
    public AlreadyExistGenreWithName(String message) {
        super(message);
    }
}
