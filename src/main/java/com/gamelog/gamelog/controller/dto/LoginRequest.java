package com.gamelog.gamelog.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "identifier is required") String identifier,
        @NotBlank(message = "password is required") String password
) {
}
