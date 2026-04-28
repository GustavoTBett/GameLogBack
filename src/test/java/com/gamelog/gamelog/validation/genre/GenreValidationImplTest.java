package com.gamelog.gamelog.validation.genre;

import com.gamelog.gamelog.exception.EntityCannotBeNull;
import com.gamelog.gamelog.exception.genre.AlreadyExistGenreWithName;
import com.gamelog.gamelog.model.Genre;
import com.gamelog.gamelog.service.genre.GenreServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenreValidationImplTest {

    @Mock
    private GenreServiceImpl genreService;

    @InjectMocks
    private GenreValidationImpl genreValidation;

    @Test
    void validateUniqueNameShouldThrowWhenGenreIsNull() {
        assertThrows(EntityCannotBeNull.class, () -> genreValidation.validateUniqueName(null));
    }

    @Test
    void validateUniqueNameShouldThrowWhenAnotherGenreWithSameNameExists() {
        Genre requestGenre = Genre.builder().id(1L).name("Action").build();
        Genre existing = Genre.builder().id(2L).name("Action").build();
        when(genreService.findByName("Action")).thenReturn(Optional.of(existing));

        assertThrows(AlreadyExistGenreWithName.class, () -> genreValidation.validateUniqueName(requestGenre));
    }

    @Test
    void validateUniqueNameShouldPassWhenGenreNotFound() {
        Genre requestGenre = Genre.builder().id(1L).name("Action").build();
        when(genreService.findByName("Action")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> genreValidation.validateUniqueName(requestGenre));
    }
}
