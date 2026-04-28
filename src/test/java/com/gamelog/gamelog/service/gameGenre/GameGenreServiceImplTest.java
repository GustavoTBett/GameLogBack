package com.gamelog.gamelog.service.gameGenre;

import com.gamelog.gamelog.model.GameGenre;
import com.gamelog.gamelog.model.GameGenreId;
import com.gamelog.gamelog.repository.GameGenreRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameGenreServiceImplTest {

    @Mock
    private GameGenreRepository gameGenreRepository;

    @InjectMocks
    private GameGenreServiceImpl gameGenreService;

    @Test
    void saveGetAndDeleteShouldDelegateToRepository() {
        GameGenreId id = GameGenreId.builder().gameId(1L).genreId(2L).build();
        GameGenre gameGenre = GameGenre.builder().id(id).build();

        when(gameGenreRepository.save(gameGenre)).thenReturn(gameGenre);
        when(gameGenreRepository.findById(id)).thenReturn(Optional.of(gameGenre));

        assertSame(gameGenre, gameGenreService.save(gameGenre));
        assertTrue(gameGenreService.get(id).isPresent());

        gameGenreService.delete(gameGenre);

        verify(gameGenreRepository).save(gameGenre);
        verify(gameGenreRepository).findById(id);
        verify(gameGenreRepository).delete(gameGenre);
    }
}
