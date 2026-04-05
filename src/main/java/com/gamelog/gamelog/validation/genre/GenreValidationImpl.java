package com.gamelog.gamelog.validation.genre;

import com.gamelog.gamelog.exception.EntityCannotBeNull;
import com.gamelog.gamelog.exception.genre.AlreadyExistGenreWithName;
import com.gamelog.gamelog.model.Genre;
import com.gamelog.gamelog.service.genre.GenreServiceImpl;

import java.util.Objects;
import java.util.Optional;

public class GenreValidationImpl implements GenreValidation{

    private final GenreServiceImpl genreService;

    public GenreValidationImpl(GenreServiceImpl genreService) {
        this.genreService = genreService;
    }

    @Override
    public void validateUniqueName(Genre genre) {
        if (genre == null) {
            throw new EntityCannotBeNull("Entity cannot be null");
        }
        Optional<Genre> optionalGenre = genreService.findByName(genre.getName());
        if (optionalGenre.isPresent() && !Objects.equals(genre.getId(), optionalGenre.get().getId())) {
            throw new AlreadyExistGenreWithName("Already exist genre with this name");
        }
    }
}
