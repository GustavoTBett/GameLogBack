package com.gamelog.gamelog.service.user;

import com.gamelog.gamelog.controller.dto.UserProfileUpdateRequest;
import com.gamelog.gamelog.model.enums.UserRole;
import com.gamelog.gamelog.model.User;
import com.gamelog.gamelog.model.UserPlatformMapping;
import com.gamelog.gamelog.model.enums.GamePlatform;
import com.gamelog.gamelog.repository.UserPlatformMappingRepository;
import com.gamelog.gamelog.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Set;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPlatformMappingRepository userPlatformMappingRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void saveShouldNormalizeEncodeAndSetDefaults() {
        User user = User.builder()
                .email("  USER@MAIL.COM ")
                .username("  player1  ")
                .password("plain-password")
                .platforms(Set.of(new UserPlatformMapping()))
                .build();

        when(passwordEncoder.encode("plain-password")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = userService.save(user);

        assertEquals("user@mail.com", saved.getEmail());
        assertEquals("player1", saved.getUsername());
        assertEquals("encoded-password", saved.getPassword());
        assertEquals(UserRole.USER, saved.getRole());
        assertNull(saved.getPlatforms());
        verify(passwordEncoder).encode("plain-password");
        verify(userRepository).save(saved);
    }

    @Test
    void saveShouldNotEncodeWhenPasswordAlreadyBcrypt() {
        String encoded = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
        User user = User.builder()
                .email("user@mail.com")
                .username("player")
                .password(encoded)
                .role(UserRole.ADMIN)
                .build();

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = userService.save(user);

        assertEquals(encoded, saved.getPassword());
        assertEquals(UserRole.ADMIN, saved.getRole());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void getShouldDelegateToRepository() {
        User user = User.builder().email("user@mail.com").username("player").password("x").build();
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        Optional<User> result = userService.get(5L);

        assertTrue(result.isPresent());
        assertSame(user, result.get());
    }

    @Test
    void getByIdentifierShouldTryEmailThenUsername() {
        User user = User.builder().email("user@mail.com").username("player").password("x").build();
        when(userRepository.findByEmailIgnoreCase("player")).thenReturn(Optional.empty());
        when(userRepository.findByUsernameIgnoreCase("player")).thenReturn(Optional.of(user));

        Optional<User> result = userService.getByIdentifier("  player ");

        assertTrue(result.isPresent());
        assertSame(user, result.get());
        verify(userRepository).findByEmailIgnoreCase("player");
        verify(userRepository).findByUsernameIgnoreCase("player");
    }

    @Test
    void updateProfileShouldNormalizeBasicFieldsAndReplacePlatforms() {
        User user = User.builder()
                .email("old@mail.com")
                .username("old")
                .password("encoded")
                .role(UserRole.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", 3L);

        UserPlatformMapping oldPlatform = UserPlatformMapping.builder()
                .user(user)
                .platform(GamePlatform.PC)
                .build();

        UserProfileUpdateRequest request = new UserProfileUpdateRequest(
                "  newPlayer  ",
                "  NEW@MAIL.COM ",
                " https://example.com/avatar.png ",
                " Bio nova ",
                Set.of(GamePlatform.PLAYSTATION, GamePlatform.NINTENDO)
        );

        when(userRepository.findById(3L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userPlatformMappingRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(3L))
                .thenReturn(List.of(oldPlatform));

        User updated = userService.updateProfile(3L, request);

        assertEquals("new@mail.com", updated.getEmail());
        assertEquals("newPlayer", updated.getUsername());
        assertEquals("https://example.com/avatar.png", updated.getAvatarUrl());
        assertEquals("Bio nova", updated.getBio());
        verify(userPlatformMappingRepository).deleteAll(List.of(oldPlatform));
        verify(userPlatformMappingRepository, times(2)).save(any(UserPlatformMapping.class));
    }

    @Test
    void deleteShouldDelegateToRepository() {
        User user = User.builder().email("user@mail.com").username("player").password("x").build();

        userService.delete(user);

        verify(userRepository).delete(user);
    }
}
