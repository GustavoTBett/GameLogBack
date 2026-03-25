package com.gamelog.gamelog.repository;

import com.gamelog.gamelog.model.GameGenre;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameGenreRepository extends JpaRepository<GameGenre, Long> {
}
