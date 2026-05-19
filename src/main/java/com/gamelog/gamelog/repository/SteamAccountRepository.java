package com.gamelog.gamelog.repository;

import com.gamelog.gamelog.model.SteamAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SteamAccountRepository extends JpaRepository<SteamAccount, Long> {

    Optional<SteamAccount> findByUserId(Long userId);

    Optional<SteamAccount> findBySteamId(String steamId);

}
