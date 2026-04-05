package com.gamelog.gamelog.controller.dto;

public record AuthUserResponse(
        Long id,
        String username,
        String email,
        String role
) {
}
