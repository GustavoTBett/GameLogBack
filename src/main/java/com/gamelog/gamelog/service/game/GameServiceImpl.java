package com.gamelog.gamelog.service.game;

import com.gamelog.gamelog.model.Game;
import com.gamelog.gamelog.repository.GameRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GameServiceImpl implements GameService{

    private final GameRepository gameRepository;

    public GameServiceImpl(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
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
}
