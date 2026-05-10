package com.gamelog.gamelog.service.user;

import com.gamelog.gamelog.controller.dto.UserProfileUpdateRequest;
import com.gamelog.gamelog.exception.EntityCannotBeNull;
import com.gamelog.gamelog.model.enums.UserRole;
import com.gamelog.gamelog.model.User;
import com.gamelog.gamelog.model.UserPlatformMapping;
import com.gamelog.gamelog.model.enums.GamePlatform;
import com.gamelog.gamelog.repository.UserPlatformMappingRepository;
import com.gamelog.gamelog.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class UserServiceImpl implements UserService{

    private static final Pattern BCRYPT_PATTERN = Pattern.compile("^\\$2[aby]?\\$.{56}$");

    private final UserRepository userRepository;
    private final UserPlatformMappingRepository userPlatformMappingRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(
            UserRepository userRepository,
            UserPlatformMappingRepository userPlatformMappingRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.userPlatformMappingRepository = userPlatformMappingRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User save(User user) {
        normalizeUser(user);

        if (user.getRole() == null) {
            user.setRole(UserRole.USER);
        }

        if (user.getGoogleEmailVerified() == null) {
            user.setGoogleEmailVerified(false);
        }

        if (user.getPlatforms() != null) {
            user.setPlatforms(null);
        }

        if (!isPasswordEncoded(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User updateProfile(Long userId, UserProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityCannotBeNull("User not found with id " + userId));

        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setAvatarUrl(blankToNull(request.avatarUrl()));
        user.setBio(blankToNull(request.bio()));

        normalizeUser(user);
        User savedUser = userRepository.save(user);
        replacePlatforms(savedUser, request.platforms());

        return savedUser;
    }

    @Override
    public Optional<User> get(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public List<GamePlatform> getPlatforms(Long userId) {
        return userPlatformMappingRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(userId)
                .stream()
                .map(UserPlatformMapping::getPlatform)
                .distinct()
                .toList();
    }

    @Override
    public Optional<User> getByIdentifier(String identifier) {
        String value = identifier == null ? "" : identifier.trim();
        return userRepository.findByEmailIgnoreCase(value)
                .or(() -> userRepository.findByUsernameIgnoreCase(value));
    }

    @Override
    public void delete(User user) {
        userRepository.delete(user);
    }

    private static void normalizeUser(User user) {
        if (user.getEmail() != null) {
            user.setEmail(user.getEmail().trim().toLowerCase());
        }

        if (user.getUsername() != null) {
            user.setUsername(user.getUsername().trim());
        }
    }

    private static boolean isPasswordEncoded(String password) {
        return password != null && BCRYPT_PATTERN.matcher(password).matches();
    }

    private void replacePlatforms(User user, Set<GamePlatform> platforms) {
        List<UserPlatformMapping> existingPlatforms =
                userPlatformMappingRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(user.getId());

        userPlatformMappingRepository.deleteAll(existingPlatforms);
        userPlatformMappingRepository.flush();

        if (platforms == null || platforms.isEmpty()) {
            return;
        }

        new LinkedHashSet<>(platforms).stream()
                .filter(Objects::nonNull)
                .map(platform -> UserPlatformMapping.builder()
                        .user(user)
                        .platform(platform)
                        .build())
                .forEach(userPlatformMappingRepository::save);
    }

    private static String blankToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }
}
