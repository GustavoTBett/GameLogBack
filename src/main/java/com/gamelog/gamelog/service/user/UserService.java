package com.gamelog.gamelog.service.user;

import com.gamelog.gamelog.model.User;

import java.util.Optional;

public interface UserService {

    User save(User user);

    Optional<User> get(Long id);

    Optional<User> getByIdentifier(String identifier);

    void delete(User user);
}
