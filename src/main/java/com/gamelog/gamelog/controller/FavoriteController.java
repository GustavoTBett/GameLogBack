package com.gamelog.gamelog.controller;

import com.gamelog.gamelog.config.security.AppUserPrincipal;
import com.gamelog.gamelog.controller.dto.FavoriteRequest;
import com.gamelog.gamelog.controller.dto.FavoriteStatusResponse;
import com.gamelog.gamelog.controller.dto.GameSummaryResponse;
import com.gamelog.gamelog.model.Favorite;
import com.gamelog.gamelog.service.favorite.FavoriteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@RestController
@RequestMapping("/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @PostMapping
    public ResponseEntity<Favorite> create(@Valid @RequestBody FavoriteRequest request, Authentication authentication) {
        assertUserCanActOnUserId(request.userId(), authentication);

        Favorite favorite = this.favoriteService.validateDtoSaveAndReturnFavorite(request);

        Favorite savedFavorite = favoriteService.save(favorite);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedFavorite.getId())
                .toUri();

        return ResponseEntity.created(location).body(savedFavorite);
    }

    @GetMapping("/me")
    public ResponseEntity<List<GameSummaryResponse>> getCurrentUserFavorites(Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);
        return ResponseEntity.ok(favoriteService.getFavoriteGames(userId));
    }

    @GetMapping("/games/{gameId}")
    public ResponseEntity<FavoriteStatusResponse> getGameFavoriteStatus(
            @PathVariable Long gameId,
            Authentication authentication
    ) {
        Long userId = getAuthenticatedUserId(authentication);

        return ResponseEntity.ok(toFavoriteStatus(gameId, favoriteService.getByUserAndGame(userId, gameId).orElse(null)));
    }

    @PostMapping("/games/{gameId}")
    public ResponseEntity<FavoriteStatusResponse> favoriteGame(
            @PathVariable Long gameId,
            Authentication authentication
    ) {
        Long userId = getAuthenticatedUserId(authentication);
        Favorite favorite = favoriteService.favoriteGame(userId, gameId);

        return ResponseEntity.ok(toFavoriteStatus(gameId, favorite));
    }

    @DeleteMapping("/games/{gameId}")
    public ResponseEntity<Void> unfavoriteGame(
            @PathVariable Long gameId,
            Authentication authentication
    ) {
        Long userId = getAuthenticatedUserId(authentication);
        favoriteService.deleteByUserAndGame(userId, gameId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Favorite> getById(@PathVariable Long id) {
        return favoriteService.get(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return favoriteService.get(id)
                .map(favorite -> {
                    favoriteService.delete(favorite);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private static void assertUserCanActOnUserId(Long requestedUserId, Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);
        boolean isAdmin = isAdmin(authentication);

        if (!isAdmin && !userId.equals(requestedUserId)) {
            throw new ResponseStatusException(FORBIDDEN, "You cannot perform this action for another user");
        }
    }

    private static Long getAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new ResponseStatusException(FORBIDDEN, "Access denied");
        }

        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        return principal.getId();
    }

    private static boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private static FavoriteStatusResponse toFavoriteStatus(Long gameId, Favorite favorite) {
        return new FavoriteStatusResponse(
                gameId,
                favorite != null,
                favorite != null ? favorite.getId() : null
        );
    }
}

