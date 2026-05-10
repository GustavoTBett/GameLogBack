package com.gamelog.gamelog.service.game;

import com.gamelog.gamelog.controller.dto.GameDetailResponse;
import com.gamelog.gamelog.controller.dto.GameSummaryResponse;
import com.gamelog.gamelog.model.enums.GamePlatform;
import com.gamelog.gamelog.model.Game;
import com.gamelog.gamelog.model.GamePlatformMapping;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface GameService {

    Game save(Game game);

    Optional<Game> get(Long id);

    Optional<GameDetailResponse> getSummaryBySlug(String slug, Long currentUserId);

    void delete(Game game);

    GamePlatformMapping addPlatform(Long gameId, GamePlatform platform);

    void removePlatform(Long gameId, GamePlatform platform);

    Set<GamePlatformMapping> getPlatforms(Long gameId);

    Page<GameSummaryResponse> explore(int page, int size, Long genreId, GamePlatform platform, Double minRating, String q);

    List<GameSummaryResponse> summarize(List<Game> games);

    List<GameSummaryResponse> getPopular(int limit);

    List<GameSummaryResponse> getTopRated(int limit);
}
