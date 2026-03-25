package com.gamelog.gamelog.service.game;

import com.gamelog.gamelog.model.Game;
import com.gamelog.gamelog.model.GameGenre;

import java.util.Optional;

public interface GameService {

    Game save(Game game);

    Optional<Game> get(Long id);

    void delete(Game game);
}
