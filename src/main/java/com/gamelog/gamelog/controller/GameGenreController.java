package com.gamelog.gamelog.controller;

import com.gamelog.gamelog.model.GameGenre;
import com.gamelog.gamelog.model.GameGenreId;
import com.gamelog.gamelog.service.gameGenre.GameGenreService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/game-genres")
public class GameGenreController {

    private final GameGenreService gameGenreService;

    public GameGenreController(GameGenreService gameGenreService) {
        this.gameGenreService = gameGenreService;
    }

    @GetMapping("/{gameId}/{genreId}")
    public ResponseEntity<GameGenre> getById(
            @PathVariable Long gameId,
            @PathVariable Long genreId
    ) {
        return gameGenreService.get(GameGenreId.builder()
                        .gameId(gameId)
                        .genreId(genreId)
                        .build())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

