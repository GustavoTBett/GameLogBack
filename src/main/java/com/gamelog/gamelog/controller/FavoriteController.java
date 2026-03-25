package com.gamelog.gamelog.controller;

import com.gamelog.gamelog.config.security.AppUserPrincipal;
import com.gamelog.gamelog.controller.dto.FavoriteRequest;
import com.gamelog.gamelog.model.Favorite;
import com.gamelog.gamelog.service.favorite.FavoriteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

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
}

