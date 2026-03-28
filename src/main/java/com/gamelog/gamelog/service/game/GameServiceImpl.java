package com.gamelog.gamelog.service.game;

import com.gamelog.gamelog.exception.EntityCannotBeNull;
import com.gamelog.gamelog.model.EnumUser.GamePlatform;
import com.gamelog.gamelog.model.Game;
import com.gamelog.gamelog.model.GamePlatformMapping;
import com.gamelog.gamelog.repository.GamePlatformMappingRepository;
import com.gamelog.gamelog.repository.GameRepository;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
public class GameServiceImpl implements GameService{

    private final GameRepository gameRepository;
    private final GamePlatformMappingRepository gamePlatformRepository;

    public GameServiceImpl(GameRepository gameRepository, GamePlatformMappingRepository gamePlatformRepository) {
        this.gameRepository = gameRepository;
        this.gamePlatformRepository = gamePlatformRepository;
    }

    @Override
    public Game save(Game game) {
        return gameRepository.save(game);
    }

    @Override
    public Optional<Game> get(Long id) {
        return gameRepository.findById(id);
    }

    @Override
    public void delete(Game game) {
        gameRepository.delete(game);
    }

    @Override
    public GamePlatformMapping addPlatform(Long gameId, GamePlatform platform) {
        Game game = get(gameId)
                .orElseThrow(() -> new EntityCannotBeNull("Game not found"));
        
        return gamePlatformRepository.save(GamePlatformMapping.builder()
                .game(game)
                .platform(platform)
                .build());
    }

    @Override
    public void removePlatform(Long gameId, GamePlatform platform) {
        gamePlatformRepository.deleteByGameIdAndPlatform(gameId, platform);
    }

    @Override
    public Set<GamePlatformMapping> getPlatforms(Long gameId) {
        Game game = get(gameId)
                .orElseThrow(() -> new EntityCannotBeNull("Game not found"));
        return game.getPlatforms() != null ? game.getPlatforms() : new HashSet<>();
    }
}
