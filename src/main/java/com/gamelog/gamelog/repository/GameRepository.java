package com.gamelog.gamelog.repository;

import com.gamelog.gamelog.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GameRepository extends JpaRepository<Game, Long>, JpaSpecificationExecutor<Game> {

	@Query(
			value = """
					SELECT g
					FROM Game g
					ORDER BY (
						SELECT COUNT(r)
						FROM Rating r
						WHERE r.game.id = g.id
					) DESC,
					g.averageRating DESC,
					g.id DESC
					""",
			countQuery = "SELECT COUNT(g) FROM Game g"
	)
	Page<Game> findPopular(Pageable pageable);
}
