package com.gamelog.gamelog.service.auth;

import com.gamelog.gamelog.model.PasswordResetToken;
import com.gamelog.gamelog.model.User;
import com.gamelog.gamelog.repository.PasswordResetTokenRepository;
import com.gamelog.gamelog.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordResetNotificationService passwordResetNotificationService;

    @Mock
    private PasswordEncoder passwordEncoder;

        private PasswordResetServiceImpl passwordResetService;

    private User user;

    @BeforeEach
    void setUp() {
        passwordResetService = new PasswordResetServiceImpl(
            userRepository,
            passwordResetTokenRepository,
            passwordResetNotificationService,
            passwordEncoder,
            "http://localhost:3000"
        );

        user = User.builder()
                .email("user@mail.com")
                .username("player")
                .password("old-pass")
                .build();
    }

    @Test
    void requestResetShouldDoNothingForBlankEmail() {
        passwordResetService.requestReset("   ");

        verifyNoInteractions(userRepository, passwordResetTokenRepository, passwordResetNotificationService);
    }

    @Test
    void requestResetShouldDoNothingWhenUserDoesNotExist() {
        when(userRepository.findByEmailIgnoreCase("missing@mail.com")).thenReturn(Optional.empty());

        passwordResetService.requestReset("missing@mail.com");

        verify(userRepository).findByEmailIgnoreCase("missing@mail.com");
        verify(passwordResetTokenRepository, never()).save(any());
        verify(passwordResetNotificationService, never()).sendResetLink(anyString(), anyString());
    }

    @Test
    void requestResetShouldCreateTokenAndSendEmail() {
        PasswordResetToken active = PasswordResetToken.builder()
                .user(user)
                .tokenHash("old-hash")
                .expiresAt(Instant.now().plusSeconds(1000))
                .build();

        when(userRepository.findByEmailIgnoreCase("user@mail.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUserAndUsedAtIsNull(user)).thenReturn(List.of(active));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        passwordResetService.requestReset(" user@mail.com ");

        verify(passwordResetTokenRepository).saveAll(anyList());
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));

        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(passwordResetNotificationService).sendResetLink(emailCaptor.capture(), linkCaptor.capture());

        assertEquals("user@mail.com", emailCaptor.getValue());
        assertTrue(linkCaptor.getValue().startsWith("http://localhost:3000/reset-password?token="));
        assertNotNull(active.getUsedAt());
    }

    @Test
    void resetPasswordShouldRejectInvalidToken() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> passwordResetService.resetPassword(" ", "123456")
        );

        assertEquals("Token inválido ou expirado", exception.getMessage());
    }

    @Test
    void resetPasswordShouldRejectShortPassword() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> passwordResetService.resetPassword("token", "123")
        );

        assertEquals("A nova senha deve ter pelo menos 6 caracteres", exception.getMessage());
    }

    @Test
    void resetPasswordShouldRejectMissingTokenHash() {
        when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> passwordResetService.resetPassword("token", "123456")
        );

        assertEquals("Token inválido ou expirado", exception.getMessage());
    }

    @Test
    void resetPasswordShouldRejectExpiredOrUsedToken() {
        PasswordResetToken token = PasswordResetToken.builder()
                .user(user)
                .tokenHash("hash")
                .expiresAt(Instant.now().minusSeconds(1))
                .usedAt(Instant.now())
                .build();

        when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> passwordResetService.resetPassword("token", "123456")
        );

        assertEquals("Token inválido ou expirado", exception.getMessage());
    }

    @Test
    void resetPasswordShouldEncodePasswordAndInvalidateActiveTokens() {
        PasswordResetToken token = PasswordResetToken.builder()
                .user(user)
                .tokenHash("hash")
                .expiresAt(Instant.now().plusSeconds(1800))
                .build();
        PasswordResetToken active = PasswordResetToken.builder()
                .user(user)
                .tokenHash("active")
                .expiresAt(Instant.now().plusSeconds(1800))
                .build();

        when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        when(passwordResetTokenRepository.findByUserAndUsedAtIsNull(user)).thenReturn(List.of(active));
        when(passwordEncoder.encode("newPassword")).thenReturn("encoded");

        passwordResetService.resetPassword("token", "newPassword");

        assertEquals("encoded", user.getPassword());
        verify(userRepository).save(user);
        verify(passwordResetTokenRepository).saveAll(anyList());
        assertNotNull(active.getUsedAt());
    }
}
