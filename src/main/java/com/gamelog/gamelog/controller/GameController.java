package com.gamelog.gamelog.controller;

import com.gamelog.gamelog.controller.dto.GameSummaryResponse;
import com.gamelog.gamelog.controller.dto.PagedResponse;
import com.gamelog.gamelog.model.EnumUser.GamePlatform;
import com.gamelog.gamelog.model.Game;
import com.gamelog.gamelog.service.game.GameService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/games")
@Validated
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping("/explore")
    public ResponseEntity<PagedResponse<GameSummaryResponse>> explore(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(50) int size,
            @RequestParam(required = false) Long genreId,
            @RequestParam(required = false) GamePlatform platform,
            @RequestParam(required = false) Double minRating
    ) {
        return ResponseEntity.ok(
                PagedResponse.fromPage(gameService.explore(page, size, genreId, platform, minRating))
        );
    }

    @GetMapping("/popular")
    public ResponseEntity<List<GameSummaryResponse>> popular(
            @RequestParam(defaultValue = "6") @Min(1) @Max(24) int limit
    ) {
        return ResponseEntity.ok(gameService.getPopular(limit));
    }

    @GetMapping("/top-rated")
    public ResponseEntity<List<GameSummaryResponse>> topRated(
            @RequestParam(defaultValue = "6") @Min(1) @Max(24) int limit
    ) {
        return ResponseEntity.ok(gameService.getTopRated(limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Game> getById(@PathVariable Long id) {
        return gameService.get(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return gameService.get(id)
                .map(game -> {
                    gameService.delete(game);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
