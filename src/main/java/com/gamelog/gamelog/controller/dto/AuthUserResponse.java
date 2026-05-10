package com.gamelog.gamelog.controller.dto;

import com.gamelog.gamelog.model.User;
import com.gamelog.gamelog.model.enums.GamePlatform;

import java.util.List;

public record AuthUserResponse(
        Long id,
        String username,
        String email,
        String role,
        String avatarUrl,
        String bio,
        List<GamePlatform> platforms
) {
    public static AuthUserResponse from(User user, List<GamePlatform> platforms) {
        return new AuthUserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                user.getAvatarUrl(),
                user.getBio(),
                platforms == null ? List.of() : platforms
        );
    }
}
