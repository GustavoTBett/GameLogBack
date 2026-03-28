package com.gamelog.gamelog.repository;

import com.gamelog.gamelog.model.EnumUser.GamePlatform;
import com.gamelog.gamelog.model.GamePlatformMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GamePlatformMappingRepository extends JpaRepository<GamePlatformMapping, Long> {
    void deleteByGameIdAndPlatform(Long gameId, GamePlatform platform);
}
