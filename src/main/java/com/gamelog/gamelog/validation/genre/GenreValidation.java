package com.gamelog.gamelog.validation.genre;

import com.gamelog.gamelog.model.Genre;

public interface GenreValidation {

    void validateUniqueName(Genre genre);
}
