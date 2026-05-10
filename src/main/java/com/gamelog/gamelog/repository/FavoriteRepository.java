package com.gamelog.gamelog.repository;

import com.gamelog.gamelog.model.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
	List<Favorite> findAllByUserIdOrderByCreatedAtDescIdDesc(Long userId);

	boolean existsByUserIdAndGameId(Long userId, Long gameId);

	Optional<Favorite> findFirstByUserIdAndGameId(Long userId, Long gameId);
}
