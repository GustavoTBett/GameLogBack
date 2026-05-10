package com.gamelog.gamelog.service.favorite;

import com.gamelog.gamelog.controller.dto.FavoriteRequest;
import com.gamelog.gamelog.controller.dto.GameSummaryResponse;
import com.gamelog.gamelog.model.Favorite;
import com.gamelog.gamelog.model.Game;
import com.gamelog.gamelog.model.User;

import java.util.List;
import java.util.Optional;

public interface FavoriteService {

    Favorite validateDtoSaveAndReturnFavorite(FavoriteRequest favoriteRequest);

    Favorite save(Favorite favorite);

    Favorite favoriteGame(Long userId, Long gameId);

    Optional<Favorite> get(Long id);

    List<GameSummaryResponse> getFavoriteGames(Long userId);

    void delete(Favorite favorite);

    void deleteByUserAndGame(Long userId, Long gameId);

    Optional<Favorite> getByUserAndGame(User user, Game game);

    Optional<Favorite> getByUserAndGame(Long userId, Long gameId);
}
