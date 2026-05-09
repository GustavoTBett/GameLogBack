package com.gamelog.gamelog.controller.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AiRecommendationResponse(
        @JsonProperty("game_name")
        String gameName,

        @JsonProperty("game_slug")
        String gameSlug,
        
        @JsonProperty("reason")
        String reason
) {}
