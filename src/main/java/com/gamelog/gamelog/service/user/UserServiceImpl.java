package com.gamelog.gamelog.service.user;

import com.gamelog.gamelog.model.User;
import com.gamelog.gamelog.model.UserRole;
import com.gamelog.gamelog.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class UserServiceImpl implements UserService{

    private static final Pattern BCRYPT_PATTERN = Pattern.compile("^\\$2[aby]?\\$.{56}$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User save(User user) {
        normalizeUser(user);

        if (user.getRole() == null) {
            user.setRole(UserRole.USER);
        }

        if (!isPasswordEncoded(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        return userRepository.save(user);
    }

    @Override
    public Optional<User> get(Long id) {
        return userRepository.findById(id);
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
}
