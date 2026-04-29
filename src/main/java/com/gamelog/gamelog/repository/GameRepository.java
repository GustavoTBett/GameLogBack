package com.gamelog.gamelog.repository;

import com.gamelog.gamelog.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface GameRepository extends JpaRepository<Game, Long>, JpaSpecificationExecutor<Game> {

	Optional<Game> findBySlug(String slug);

	@Query(
			value = """
					SELECT g
					FROM Game g
					LEFT JOIN Rating r ON r.game = g
					GROUP BY g
					ORDER BY COUNT(r) DESC, g.averageRating DESC, g.id DESC
					""",
			countQuery = "SELECT COUNT(g) FROM Game g"
	)
	Page<Game> findPopular(Pageable pageable);
}
