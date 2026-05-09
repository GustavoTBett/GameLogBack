package com.gamelog.gamelog.controller.dto;

public record FavoriteStatusResponse(
        Long gameId,
        boolean favorite,
        Long favoriteId
) {
}
