package com.gamelog.gamelog.controller;

import com.gamelog.gamelog.config.security.AppUserPrincipal;
import com.gamelog.gamelog.controller.dto.RatingRequest;
import com.gamelog.gamelog.model.Rating;
import com.gamelog.gamelog.controller.dto.GameReviewResponse;
import com.gamelog.gamelog.service.rating.RatingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@RestController
@RequestMapping("/ratings")
public class RatingController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @PostMapping
    public ResponseEntity<GameReviewResponse> create(@Valid @RequestBody RatingRequest request, Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);

        Rating rating = this.ratingService.buildRating(request, userId);

        Rating savedRating = ratingService.save(rating);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedRating.getId())
                .toUri();

        String username = savedRating.getUser() != null ? savedRating.getUser().getUsername() : null;
        GameReviewResponse resp = new GameReviewResponse(
            savedRating.getId(),
            savedRating.getScore(),
            savedRating.getReview(),
            username,
            savedRating.getCreatedAt()
        );

        return ResponseEntity.created(location).body(resp);
    }

        @GetMapping("/{id}")
        public ResponseEntity<GameReviewResponse> getById(@PathVariable Long id) {
        return ratingService.get(id)
            .map(r -> {
                String u = r.getUser() != null ? r.getUser().getUsername() : null;
                return ResponseEntity.ok(new GameReviewResponse(r.getId(), r.getScore(), r.getReview(), u, r.getCreatedAt()));
            })
            .orElseGet(() -> ResponseEntity.notFound().build());
        }

    @PutMapping("/{id}")
        public ResponseEntity<GameReviewResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody RatingRequest request,
            Authentication authentication
    ) {
        Long userId = getAuthenticatedUserId(authentication);

        return ratingService.get(id)
                .map(existingRating -> {
                    Rating rating = this.ratingService.buildRating(request, userId);

                    existingRating.setUser(rating.getUser());
                    existingRating.setGame(rating.getGame());
                    existingRating.setScore(rating.getScore());
                    existingRating.setReview(rating.getReview());

                    Rating saved = ratingService.save(existingRating);
                    String u = saved.getUser() != null ? saved.getUser().getUsername() : null;
                    return ResponseEntity.ok(new GameReviewResponse(saved.getId(), saved.getScore(), saved.getReview(), u, saved.getCreatedAt()));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private static Long getAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new ResponseStatusException(FORBIDDEN, "Access denied");
        }

        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        return principal.getId();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return ratingService.get(id)
                .map(rating -> {
                    ratingService.delete(rating);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

