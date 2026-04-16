package com.gamelog.gamelog.service.gameGenre;

import com.gamelog.gamelog.model.GameGenre;
import com.gamelog.gamelog.model.GameGenreId;

import java.util.Optional;

public interface GameGenreService {

    GameGenre save(GameGenre gameGenre);

    Optional<GameGenre> get(GameGenreId id);

    void delete(GameGenre gameGenre);
}
