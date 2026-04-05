package com.gamelog.gamelog.service.auth;

import com.gamelog.gamelog.model.PasswordResetToken;
import com.gamelog.gamelog.model.User;
import com.gamelog.gamelog.repository.PasswordResetTokenRepository;
import com.gamelog.gamelog.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final Duration TOKEN_EXPIRATION = Duration.ofMinutes(30);

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordResetNotificationService passwordResetNotificationService;
    private final PasswordEncoder passwordEncoder;
    private final String frontendBaseUrl;

    public PasswordResetServiceImpl(
            UserRepository userRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordResetNotificationService passwordResetNotificationService,
            PasswordEncoder passwordEncoder,
            @Value("${app.frontend.base-url:http://localhost:3000}") String frontendBaseUrl
    ) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordResetNotificationService = passwordResetNotificationService;
        this.passwordEncoder = passwordEncoder;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Override
    @Transactional
    public void requestReset(String email) {
        if (email == null || email.isBlank()) {
            return;
        }

        String normalizedEmail = email.trim().toLowerCase();
        Optional<User> userOptional = userRepository.findByEmailIgnoreCase(normalizedEmail);

        if (userOptional.isEmpty()) {
            return;
        }

        User user = userOptional.get();
        invalidateActiveTokens(user);

        String rawToken = generateToken();
        String tokenHash = hashToken(rawToken);

        PasswordResetToken passwordResetToken = PasswordResetToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plus(TOKEN_EXPIRATION))
                .build();

        passwordResetTokenRepository.save(passwordResetToken);

        String resetLink = frontendBaseUrl + "/reset-password?token=" + rawToken;
        passwordResetNotificationService.sendResetLink(normalizedEmail, resetLink);
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token inválido ou expirado");
        }

        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("A nova senha deve ter pelo menos 6 caracteres");
        }

        String tokenHash = hashToken(token);
        PasswordResetToken passwordResetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Token inválido ou expirado"));

        if (passwordResetToken.getUsedAt() != null || passwordResetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Token inválido ou expirado");
        }

        User user = passwordResetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        invalidateActiveTokens(user);
    }

    private void invalidateActiveTokens(User user) {
        List<PasswordResetToken> activeTokens = passwordResetTokenRepository.findByUserAndUsedAtIsNull(user);
        if (activeTokens.isEmpty()) {
            return;
        }

        Instant now = Instant.now();
        activeTokens.forEach(token -> token.setUsedAt(now));
        passwordResetTokenRepository.saveAll(activeTokens);
    }

    private static String generateToken() {
        byte[] randomBytes = new byte[32];
        new java.security.SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private static String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Could not hash token", exception);
        }
    }
}
