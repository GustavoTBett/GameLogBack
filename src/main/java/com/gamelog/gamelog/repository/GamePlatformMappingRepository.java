package com.gamelog.gamelog.repository;

import com.gamelog.gamelog.model.EnumUser.GamePlatform;
import com.gamelog.gamelog.model.GamePlatformMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface GamePlatformMappingRepository extends JpaRepository<GamePlatformMapping, Long> {

    void deleteByGameIdAndPlatform(Long gameId, GamePlatform platform);

    List<GamePlatformMapping> findAllByGameIdIn(Collection<Long> gameIds);
}
