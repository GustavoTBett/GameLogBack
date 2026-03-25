package com.gamelog.gamelog.service.favorite;

import com.gamelog.gamelog.controller.dto.FavoriteRequest;
import com.gamelog.gamelog.model.Favorite;
import com.gamelog.gamelog.model.Game;
import com.gamelog.gamelog.model.User;

import java.util.Optional;

public interface FavoriteService {

    Favorite validateDtoSaveAndReturnFavorite(FavoriteRequest favoriteRequest);

    Favorite save(Favorite favorite);

    Optional<Favorite> get(Long id);

    void delete(Favorite favorite);

    Optional<Favorite> getByUserAndGame(User user, Game game);
}
