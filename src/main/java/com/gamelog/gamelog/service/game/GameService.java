package com.gamelog.gamelog.service.game;

import com.gamelog.gamelog.model.EnumUser.GamePlatform;
import com.gamelog.gamelog.model.Game;
import com.gamelog.gamelog.model.GamePlatformMapping;

import java.util.Optional;
import java.util.Set;

public interface GameService {

    Game save(Game game);

    Optional<Game> get(Long id);

    void delete(Game game);

    GamePlatformMapping addPlatform(Long gameId, GamePlatform platform);

    void removePlatform(Long gameId, GamePlatform platform);

    Set<GamePlatformMapping> getPlatforms(Long gameId);
}
