package com.gamelog.gamelog.validation.genre;

import com.gamelog.gamelog.exception.EntityCannotBeNull;
import com.gamelog.gamelog.exception.genre.AlreadyExistGenreWithName;
import com.gamelog.gamelog.model.Genre;
import com.gamelog.gamelog.repository.GenreRepository;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

@Component
public class GenreValidationImpl implements GenreValidation {

    private final GenreRepository genreRepository;

    public GenreValidationImpl(GenreRepository genreRepository) {
        this.genreRepository = genreRepository;
    }

    @Override
    public void validateUniqueName(Genre genre) {
        if (genre == null) {
            throw new EntityCannotBeNull("Entity cannot be null");
        }
        Optional<Genre> optionalGenre = genreRepository.findByName(genre.getName());
        if (optionalGenre.isPresent() && !Objects.equals(genre.getId(), optionalGenre.get().getId())) {
            throw new AlreadyExistGenreWithName("Already exist genre with this name");
        }
    }
}
