package com.gamelog.gamelog.service.gameGenre;

import com.gamelog.gamelog.model.GameGenre;
import com.gamelog.gamelog.repository.GameGenreRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GameGenreServiceImpl implements GameGenreService {

    private final GameGenreRepository gameGenreRepository;

    public GameGenreServiceImpl(GameGenreRepository gameGenreRepository) {
        this.gameGenreRepository = gameGenreRepository;
    }

    @Override
    public GameGenre save(GameGenre gameGenre) {
        return gameGenreRepository.save(gameGenre);
    }

    @Override
    public Optional<GameGenre> get(Long id) {
        return gameGenreRepository.findById(id);
    }

    @Override
    public void delete(GameGenre gameGenre) {
        gameGenreRepository.delete(gameGenre);
    }
}
