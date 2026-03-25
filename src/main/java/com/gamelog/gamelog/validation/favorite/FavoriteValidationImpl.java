package com.gamelog.gamelog.validation.favorite;

import com.gamelog.gamelog.exception.EntityCannotBeNull;
import com.gamelog.gamelog.exception.favorite.AlreadyExistFavoriteWithUserAndGame;
import com.gamelog.gamelog.model.Favorite;
import com.gamelog.gamelog.service.favorite.FavoriteService;

import java.util.Objects;
import java.util.Optional;

public class FavoriteValidationImpl implements FavoriteValidation {

    private final FavoriteService favoriteService;

    public FavoriteValidationImpl(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @Override
    public void validateUniqueUserGame(Favorite favorite) {
        if (favorite == null) {
            throw new EntityCannotBeNull("Entiy cannot be null");
        }
        Optional<Favorite> optionalFavorite = favoriteService.getByUserAndGame(favorite.getUser(), favorite.getGame());
        if (optionalFavorite.isPresent() && !Objects.equals(optionalFavorite.get().getId(), favorite.getId())) {
            throw new AlreadyExistFavoriteWithUserAndGame("Already exist a favorite with this user and game");
        }
    }
}
