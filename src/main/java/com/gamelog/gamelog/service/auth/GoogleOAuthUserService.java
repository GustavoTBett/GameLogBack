package com.gamelog.gamelog.service.auth;

import com.gamelog.gamelog.model.User;
import com.gamelog.gamelog.repository.UserRepository;
import com.gamelog.gamelog.service.user.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class GoogleOAuthUserService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public GoogleOAuthUserService(
            UserRepository userRepository,
            UserService userService,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    public User synchronize(OidcUser oidcUser) {
        String googleSub = oidcUser.getSubject();
        String email = normalize(oidcUser.getEmail());
        boolean emailVerified = Boolean.TRUE.equals(oidcUser.getClaims().get("email_verified"));
        String pictureUrl = getStringClaim(oidcUser, "picture");

        User user = userRepository.findByGoogleSub(googleSub)
                .or(() -> emailVerified ? userRepository.findByEmailIgnoreCase(email) : Optional.empty())
                .map(existingUser -> updateExistingUser(existingUser, googleSub, emailVerified, pictureUrl))
                .orElseGet(() -> createNewUser(oidcUser, googleSub, email, emailVerified, pictureUrl));

        return userService.save(user);
    }

    private User updateExistingUser(User user, String googleSub, boolean emailVerified, String pictureUrl) {
        user.setGoogleSub(googleSub);
        user.setGoogleEmailVerified(emailVerified);

        if (isBlank(user.getAvatarUrl()) && !isBlank(pictureUrl)) {
            user.setAvatarUrl(pictureUrl);
        }

        return user;
    }

    private User createNewUser(
            OidcUser oidcUser,
            String googleSub,
            String email,
            boolean emailVerified,
            String pictureUrl
    ) {
        String baseUsername = firstNonBlank(
                getStringClaim(oidcUser, "preferred_username"),
                getStringClaim(oidcUser, "name"),
                email.contains("@") ? email.substring(0, email.indexOf('@')) : email,
                "google-user"
        );

        String username = buildUniqueUsername(baseUsername);

        return User.builder()
                .email(email)
                .username(username)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .avatarUrl(pictureUrl)
                .googleSub(googleSub)
                .googleEmailVerified(emailVerified)
                .build();
    }

    private String buildUniqueUsername(String baseUsername) {
        String normalizedBase = sanitizeUsername(baseUsername);
        if (normalizedBase.isBlank()) {
            normalizedBase = "google-user";
        }

        String candidate = normalizedBase;
        int suffix = 1;
        while (userRepository.existsByUsernameIgnoreCase(candidate)) {
            candidate = normalizedBase + suffix;
            suffix++;
        }

        return candidate;
    }

    private static String sanitizeUsername(String value) {
        return value == null
                ? ""
                : value.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }

        return "google-user";
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String normalize(String value) {
        if (value == null) {
            throw new IllegalStateException("Google account returned no email");
        }

        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static String getStringClaim(OidcUser oidcUser, String claimName) {
        Object claim = oidcUser.getClaims().get(claimName);
        return claim instanceof String stringValue ? stringValue : null;
    }
}