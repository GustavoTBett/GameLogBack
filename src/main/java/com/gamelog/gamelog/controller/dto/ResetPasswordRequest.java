package com.gamelog.gamelog.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "token is required") String token,
        @NotBlank(message = "newPassword is required")
        @Size(min = 6, message = "newPassword must contain at least 6 characters")
        String newPassword
) {
}
