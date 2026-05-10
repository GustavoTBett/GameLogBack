package com.gamelog.gamelog.service.genre;

import com.gamelog.gamelog.model.Genre;
import com.gamelog.gamelog.repository.GenreRepository;
import com.gamelog.gamelog.validation.genre.GenreValidation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenreServiceImplTest {

    @Mock
    private GenreRepository genreRepository;

    @Mock
    private GenreValidation genreValidation;

    @InjectMocks
    private GenreServiceImpl genreService;

    @Test
    void saveGetDeleteAndFindAllShouldDelegateToRepository() {
        Genre genre = Genre.builder().id(1L).name("Action").build();
        when(genreRepository.save(genre)).thenReturn(genre);
        when(genreRepository.findById(1L)).thenReturn(Optional.of(genre));
        when(genreRepository.findAll()).thenReturn(List.of(genre));

        assertSame(genre, genreService.save(genre));
        assertTrue(genreService.get(1L).isPresent());
        assertEquals(1, genreService.findAll().size());

        genreService.delete(genre);

        verify(genreRepository).save(genre);
        verify(genreValidation).validateUniqueName(genre);
        verify(genreRepository).findById(1L);
        verify(genreRepository).findAll();
        verify(genreRepository).delete(genre);
    }

    @Test
    void findByNameShouldDelegateToRepository() {
        Genre genre = Genre.builder().id(1L).name("Action").build();
        when(genreRepository.findByName("Action")).thenReturn(Optional.of(genre));

        Optional<Genre> result = genreService.findByName("Action");

        assertTrue(result.isPresent());
        assertSame(genre, result.get());
        verify(genreRepository).findByName("Action");
    }
}
