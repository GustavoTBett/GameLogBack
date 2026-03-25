package com.gamelog.gamelog.controller.dto;

public record CsrfTokenResponse(
        String headerName,
        String token
) {
}
