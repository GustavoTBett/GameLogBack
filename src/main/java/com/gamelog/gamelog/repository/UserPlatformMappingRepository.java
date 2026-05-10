package com.gamelog.gamelog.repository;

import com.gamelog.gamelog.model.enums.GamePlatform;
import com.gamelog.gamelog.model.UserPlatformMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserPlatformMappingRepository extends JpaRepository<UserPlatformMapping, Long> {
	List<UserPlatformMapping> findAllByUserIdOrderByCreatedAtDescIdDesc(Long userId);

    void deleteByUserIdAndPlatform(Long userId, GamePlatform platform);
}
