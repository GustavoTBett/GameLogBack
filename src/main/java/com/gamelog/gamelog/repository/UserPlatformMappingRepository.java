package com.gamelog.gamelog.repository;

import com.gamelog.gamelog.model.EnumUser.GamePlatform;
import com.gamelog.gamelog.model.UserPlatformMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPlatformMappingRepository extends JpaRepository<UserPlatformMapping, Long> {
    void deleteByUserIdAndPlatform(Long userId, GamePlatform platform);
}
