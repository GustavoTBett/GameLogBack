package com.gamelog.gamelog.controller.dto;

import jakarta.validation.constraints.NotNull;

public record FavoriteRequest(
        @NotNull(message = "userId is required") Long userId,
        @NotNull(message = "gameId is required") Long gameId
) {
}

