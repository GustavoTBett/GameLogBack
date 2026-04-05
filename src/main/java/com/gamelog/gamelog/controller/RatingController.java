package com.gamelog.gamelog.controller;

import com.gamelog.gamelog.config.security.AppUserPrincipal;
import com.gamelog.gamelog.controller.dto.RatingRequest;
import com.gamelog.gamelog.model.Rating;
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
    public ResponseEntity<Rating> create(@Valid @RequestBody RatingRequest request, Authentication authentication) {
        assertUserCanActOnUserId(request.userId(), authentication);

        Rating rating = this.ratingService.validateDtoSaveAndReturnRating(request);

        Rating savedRating = ratingService.save(rating);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedRating.getId())
                .toUri();

        return ResponseEntity.created(location).body(savedRating);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Rating> getById(@PathVariable Long id) {
        return ratingService.get(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Rating> update(
            @PathVariable Long id,
            @Valid @RequestBody RatingRequest request,
            Authentication authentication
    ) {
        assertUserCanActOnUserId(request.userId(), authentication);

        return ratingService.get(id)
                .map(existingRating -> {
                    Rating rating = this.ratingService.validateDtoSaveAndReturnRating(request);

                    existingRating.setUser(rating.getUser());
                    existingRating.setGame(rating.getGame());
                    existingRating.setScore(rating.getScore());
                    existingRating.setReview(rating.getReview());

                    return ResponseEntity.ok(ratingService.save(existingRating));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private static void assertUserCanActOnUserId(Long requestedUserId, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new ResponseStatusException(FORBIDDEN, "Access denied");
        }

        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

        if (!isAdmin && !principal.getId().equals(requestedUserId)) {
            throw new ResponseStatusException(FORBIDDEN, "You cannot perform this action for another user");
        }
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

