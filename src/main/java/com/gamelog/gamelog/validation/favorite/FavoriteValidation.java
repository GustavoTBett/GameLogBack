package com.gamelog.gamelog.validation.favorite;

import com.gamelog.gamelog.model.Favorite;

public interface FavoriteValidation {

    void validateUniqueUserGame(Favorite favorite);
}
