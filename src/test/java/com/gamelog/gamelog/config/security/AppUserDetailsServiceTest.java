package com.gamelog.gamelog.config.security;

import com.gamelog.gamelog.model.EnumUser.UserRole;
import com.gamelog.gamelog.model.User;
import com.gamelog.gamelog.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AppUserDetailsService appUserDetailsService;

    @Test
    void loadUserByUsernameShouldFindByEmailFirst() {
        User user = User.builder()
                .email("user@mail.com")
                .username("player")
                .password("encoded")
                .role(UserRole.USER)
                .build();

        when(userRepository.findByEmailIgnoreCase("user@mail.com")).thenReturn(Optional.of(user));

        AppUserPrincipal principal = (AppUserPrincipal) appUserDetailsService.loadUserByUsername("user@mail.com");

        assertEquals("player", principal.getUsername());
        assertEquals("USER", principal.getRole());
    }

    @Test
    void loadUserByUsernameShouldFallbackToUsername() {
        User user = User.builder()
                .email("user@mail.com")
                .username("player")
                .password("encoded")
                .role(UserRole.ADMIN)
                .build();

        when(userRepository.findByEmailIgnoreCase("player")).thenReturn(Optional.empty());
        when(userRepository.findByUsernameIgnoreCase("player")).thenReturn(Optional.of(user));

        AppUserPrincipal principal = (AppUserPrincipal) appUserDetailsService.loadUserByUsername("player");

        assertEquals("user@mail.com", principal.getEmail());
        assertEquals("ADMIN", principal.getRole());
    }

    @Test
    void loadUserByUsernameShouldThrowWhenUserMissing() {
        when(userRepository.findByEmailIgnoreCase("missing")).thenReturn(Optional.empty());
        when(userRepository.findByUsernameIgnoreCase("missing")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> appUserDetailsService.loadUserByUsername("missing"));
    }
}
