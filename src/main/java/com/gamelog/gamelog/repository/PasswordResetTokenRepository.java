package com.gamelog.gamelog.repository;

import com.gamelog.gamelog.model.PasswordResetToken;
import com.gamelog.gamelog.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    List<PasswordResetToken> findByUserAndUsedAtIsNull(User user);
}
