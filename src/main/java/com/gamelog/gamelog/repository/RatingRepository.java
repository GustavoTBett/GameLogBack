package com.gamelog.gamelog.repository;

import com.gamelog.gamelog.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    List<Rating> findAllByGameIdOrderByCreatedAtDescIdDesc(Long gameId);

	@Query("""
			SELECT r.game.id, COUNT(r.id)
			FROM Rating r
			WHERE r.game.id IN :gameIds
			GROUP BY r.game.id
			""")
	List<Object[]> countRatingsByGameIds(@Param("gameIds") Collection<Long> gameIds);
}
