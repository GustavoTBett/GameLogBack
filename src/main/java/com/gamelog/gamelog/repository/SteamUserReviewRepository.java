package com.gamelog.gamelog.repository;

import com.gamelog.gamelog.model.SteamAccount;
import com.gamelog.gamelog.model.SteamUserReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SteamUserReviewRepository extends JpaRepository<SteamUserReview, Long> {

    Optional<SteamUserReview> findBySteamAccountAndAppId(SteamAccount account, Long appId);

    List<SteamUserReview> findAllBySteamAccount(SteamAccount account);

    List<SteamUserReview> findAllByGameIdAndActiveTrue(Long gameId);

    List<SteamUserReview> findAllByGameIdInAndActiveTrue(Collection<Long> gameIds);
}
