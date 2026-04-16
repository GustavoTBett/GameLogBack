package com.gamelog.gamelog.repository;

import com.gamelog.gamelog.model.GameGenre;
import com.gamelog.gamelog.model.GameGenreId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface GameGenreRepository extends JpaRepository<GameGenre, GameGenreId> {

	@Query("""
			SELECT gg
			FROM GameGenre gg
			JOIN FETCH gg.genre
			WHERE gg.game.id IN :gameIds
			""")
	List<GameGenre> findAllByGameIdsWithGenre(@Param("gameIds") Collection<Long> gameIds);
}
