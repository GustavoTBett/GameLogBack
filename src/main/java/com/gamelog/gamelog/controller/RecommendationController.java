package com.gamelog.gamelog.controller;

import com.gamelog.gamelog.config.security.AppUserPrincipal;
import com.gamelog.gamelog.controller.dto.RecommendationResponse;
import com.gamelog.gamelog.exception.InsufficientRatingsException;
import com.gamelog.gamelog.exception.recommendation.RecommendationServiceUnavailableException;
import com.gamelog.gamelog.service.recommendation.RecommendationGenerationOrchestrator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@RestController
@RequestMapping("/recommendations")
@Slf4j
public class RecommendationController {

    private final RecommendationGenerationOrchestrator recommendationGenerationOrchestrator;

    public RecommendationController(RecommendationGenerationOrchestrator recommendationGenerationOrchestrator) {
        this.recommendationGenerationOrchestrator = recommendationGenerationOrchestrator;
    }

    /**
     * Gera uma recomendação personalizada baseada no histórico de ratings do usuário.
     * 
     * @param authentication Autenticação do usuário (obrigatório)
     * @return Recomendação com nome do jogo, razão e detalhes
     */
    @PostMapping("/generate")
    public ResponseEntity<?> generateRecommendation(Authentication authentication) {
        try {
            Long userId = getAuthenticatedUserId(authentication);
            log.info("Generating recommendation for user {}", userId);

            RecommendationResponse recommendation = recommendationGenerationOrchestrator.generateRecommendation(userId);
            
            log.info("Recommendation generated successfully for user {}: {}", userId, recommendation.gameName());
            return ResponseEntity.ok(recommendation);
        } catch (InsufficientRatingsException e) {
            log.warn("Insufficient ratings for recommendation: {}", e.getMessage());
            return ResponseEntity
                    .status(BAD_REQUEST)
                    .body(new ErrorResponse("Insufficient ratings", e.getMessage()));
        } catch (RecommendationServiceUnavailableException e) {
            log.warn("Recommendation service unavailable: {}", e.getMessage());
            return ResponseEntity
                .status(SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("Recommendation temporarily unavailable", e.getMessage()));
        } catch (Exception e) {
            log.error("Error generating recommendation", e);
            return ResponseEntity
                    .status(INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error generating recommendation", e.getMessage()));
        }
    }

    private Long getAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("User not authenticated");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof AppUserPrincipal) {
            return ((AppUserPrincipal) principal).getId();
        }

        throw new IllegalArgumentException("Invalid authentication principal");
    }

    /**
     * DTO simples para erros
     */
    public static class ErrorResponse {
        public String error;
        public String message;

        public ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }
    }
}
