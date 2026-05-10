package com.gamelog.gamelog.controller.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AiRecommendationCandidatesResponse(
        @JsonProperty("candidates")
        List<AiRecommendationResponse> candidates
) {}
