package com.gamelog.gamelog.controller;

import com.gamelog.gamelog.config.security.AppUserPrincipal;
import com.gamelog.gamelog.controller.dto.RatingRequest;
import com.gamelog.gamelog.controller.dto.RatingVoteRequest;
import com.gamelog.gamelog.model.Rating;
import com.gamelog.gamelog.controller.dto.GameReviewResponse;
import com.gamelog.gamelog.service.rating.RatingService;
import com.gamelog.gamelog.service.ratingvote.RatingVoteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Objects;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@RestController
@RequestMapping("/ratings")
public class RatingController {

    private final RatingService ratingService;
    private final RatingVoteService ratingVoteService;

    public RatingController(RatingService ratingService, RatingVoteService ratingVoteService) {
        this.ratingService = ratingService;
        this.ratingVoteService = ratingVoteService;
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

        GameReviewResponse resp = toResponse(savedRating, userId);

        return ResponseEntity.created(location).body(resp);
    }

        @GetMapping("/{id}")
        public ResponseEntity<GameReviewResponse> getById(@PathVariable Long id) {
        return ratingService.get(id)
            .map(r -> {
                return ResponseEntity.ok(toResponse(r, null));
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
                    assertOwner(existingRating, userId);
                    existingRating.setScore(request.score());
                    existingRating.setReview(request.review());

                    Rating saved = ratingService.save(existingRating);
                    return ResponseEntity.ok(toResponse(saved, userId));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/vote")
    public ResponseEntity<Void> vote(@PathVariable Long id, @Valid @RequestBody RatingVoteRequest request, Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);
        ratingVoteService.vote(id, userId, request.voteType());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/vote")
    public ResponseEntity<Void> removeVote(@PathVariable Long id, Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);
        ratingVoteService.removeVote(id, userId);
        return ResponseEntity.noContent().build();
    }

    private static Long getAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new ResponseStatusException(FORBIDDEN, "Access denied");
        }

        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        return principal.getId();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);

        return ratingService.get(id)
                .map(rating -> {
                    assertOwner(rating, userId);
                    ratingService.delete(rating);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private static GameReviewResponse toResponse(Rating rating, Long currentUserId) {
        String username = rating.getUser() != null ? rating.getUser().getUsername() : null;
        Long ownerId = rating.getUser() != null ? rating.getUser().getId() : null;
        boolean canEdit = currentUserId != null && Objects.equals(ownerId, currentUserId);
        boolean canVote = currentUserId != null && ownerId != null && !Objects.equals(ownerId, currentUserId);

        return new GameReviewResponse(
                rating.getId(),
                rating.getScore(),
                rating.getReview(),
                username,
                rating.getCreatedAt(),
                rating.getUpdatedAt(),
                0L,
                0L,
                null,
                "APP",
                null,
                canEdit,
                canVote
        );
    }

    private static void assertOwner(Rating rating, Long userId) {
        Long ownerId = rating.getUser() != null ? rating.getUser().getId() : null;
        if (!Objects.equals(ownerId, userId)) {
            throw new ResponseStatusException(FORBIDDEN, "Access denied");
        }
    }
}

