package com.gamelog.gamelog.service.gameGenre;

import com.gamelog.gamelog.model.GameGenre;

import java.util.Optional;

public interface GameGenreService {

    GameGenre save(GameGenre gameGenre);

    Optional<GameGenre> get(Long id);

    void delete(GameGenre gameGenre);
}
