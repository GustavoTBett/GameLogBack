package com.gamelog.gamelog.repository;

import com.gamelog.gamelog.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<Game, Long> {
}
