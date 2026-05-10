package com.gamelog.gamelog.controller.dto;

import com.gamelog.gamelog.model.enums.GamePlatform;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Set;

public record UserProfileUpdateRequest(
        @NotBlank
        String username,

        @NotBlank
        @Email
        String email,

        String avatarUrl,
        String bio,
        Set<GamePlatform> platforms
) {
}
