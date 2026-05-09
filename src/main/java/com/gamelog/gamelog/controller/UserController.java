package com.gamelog.gamelog.controller;

import com.gamelog.gamelog.config.security.AppUserPrincipal;
import com.gamelog.gamelog.controller.dto.AuthUserResponse;
import com.gamelog.gamelog.controller.dto.UserProfileUpdateRequest;
import com.gamelog.gamelog.model.enums.UserRole;
import com.gamelog.gamelog.model.User;
import com.gamelog.gamelog.service.user.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<User> create(@Valid @RequestBody User user) {
        user.setRole(UserRole.USER);
        User savedUser = userService.save(user);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedUser.getId())
                .toUri();
        return ResponseEntity.created(location).body(savedUser);
    }

    @GetMapping("/me")
    public ResponseEntity<AuthUserResponse> getCurrentUser(Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);

        return userService.get(userId)
                .map(user -> ResponseEntity.ok(AuthUserResponse.from(user, userService.getPlatforms(userId))))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/me")
    public ResponseEntity<AuthUserResponse> updateCurrentUser(
            Authentication authentication,
            @Valid @RequestBody UserProfileUpdateRequest request
    ) {
        Long userId = getAuthenticatedUserId(authentication);
        User updatedUser = userService.updateProfile(userId, request);

        return ResponseEntity.ok(AuthUserResponse.from(updatedUser, userService.getPlatforms(userId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getById(@PathVariable Long id) {
        return userService.get(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> update(@PathVariable Long id, @Valid @RequestBody User user) {
        return userService.get(id)
                .map(existingUser -> {
                    existingUser.setEmail(user.getEmail());
                    existingUser.setUsername(user.getUsername());
                    existingUser.setPassword(user.getPassword());
                    existingUser.setAvatarUrl(user.getAvatarUrl());
                    existingUser.setBio(user.getBio());
                    existingUser.setRole(user.getRole());
                    existingUser.setPlatforms(user.getPlatforms());
                    return ResponseEntity.ok(userService.save(existingUser));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return userService.get(id)
                .map(user -> {
                    userService.delete(user);
                    return ResponseEntity.noContent().<Void>build();
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
}
