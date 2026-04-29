package com.gamelog.gamelog.repository;

import com.gamelog.gamelog.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {

	Optional<Rating> findFirstByUserIdAndGameId(Long userId, Long gameId);

    List<Rating> findAllByGameIdOrderByCreatedAtDescIdDesc(Long gameId);

	@Query("""
			SELECT COALESCE(AVG(r.score), 0)
			FROM Rating r
			WHERE r.game.id = :gameId
			""")
	Double findAverageScoreByGameId(@Param("gameId") Long gameId);

	@Query("""
			SELECT r.game.id, COUNT(r.id)
			FROM Rating r
			WHERE r.game.id IN :gameIds
			GROUP BY r.game.id
			""")
	List<Object[]> countRatingsByGameIds(@Param("gameIds") Collection<Long> gameIds);
}
