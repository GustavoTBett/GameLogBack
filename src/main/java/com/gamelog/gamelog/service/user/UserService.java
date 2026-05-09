package com.gamelog.gamelog.service.user;

import com.gamelog.gamelog.controller.dto.UserProfileUpdateRequest;
import com.gamelog.gamelog.model.User;
import com.gamelog.gamelog.model.enums.GamePlatform;

import java.util.List;
import java.util.Optional;

public interface UserService {

    User save(User user);

    User updateProfile(Long userId, UserProfileUpdateRequest request);

    Optional<User> get(Long id);

    List<GamePlatform> getPlatforms(Long userId);

    Optional<User> getByIdentifier(String identifier);

    void delete(User user);
}
