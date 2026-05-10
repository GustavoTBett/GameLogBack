package com.gamelog.gamelog.validation.favorite;

import com.gamelog.gamelog.exception.EntityCannotBeNull;
import com.gamelog.gamelog.exception.favorite.AlreadyExistFavoriteWithUserAndGame;
import com.gamelog.gamelog.model.Favorite;
import com.gamelog.gamelog.repository.FavoriteRepository;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

@Component
public class FavoriteValidationImpl implements FavoriteValidation {

    private final FavoriteRepository favoriteRepository;

    public FavoriteValidationImpl(FavoriteRepository favoriteRepository) {
        this.favoriteRepository = favoriteRepository;
    }

    @Override
    public void validateUniqueUserGame(Favorite favorite) {
        if (favorite == null) {
            throw new EntityCannotBeNull("Entity cannot be null");
        }
        Optional<Favorite> optionalFavorite = favoriteRepository.findFirstByUserIdAndGameId(
                favorite.getUser().getId(),
                favorite.getGame().getId()
        );
        if (optionalFavorite.isPresent() && !Objects.equals(optionalFavorite.get().getId(), favorite.getId())) {
            throw new AlreadyExistFavoriteWithUserAndGame("Already exist a favorite with this user and game");
        }
    }
}
