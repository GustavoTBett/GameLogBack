package com.gamelog.gamelog.service.favorite;

import com.gamelog.gamelog.controller.dto.FavoriteRequest;
import com.gamelog.gamelog.exception.EntityCannotBeNull;
import com.gamelog.gamelog.model.Favorite;
import com.gamelog.gamelog.model.Game;
import com.gamelog.gamelog.model.User;
import com.gamelog.gamelog.repository.FavoriteRepository;
import com.gamelog.gamelog.service.game.GameService;
import com.gamelog.gamelog.service.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Example;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceImplTest {

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private UserService userService;

    @Mock
    private GameService gameService;

    @InjectMocks
    private FavoriteServiceImpl favoriteService;

    @Test
    void validateDtoShouldBuildFavoriteWhenUserAndGameExist() {
        User user = User.builder().email("u@mail.com").username("u").password("x").build();
        Game game = Game.builder().name("Game").slug("game").averageRating(0.0).defaultRating(0.0).build();
        FavoriteRequest request = new FavoriteRequest(1L, 2L);

        when(userService.get(1L)).thenReturn(Optional.of(user));
        when(gameService.get(2L)).thenReturn(Optional.of(game));

        Favorite favorite = favoriteService.validateDtoSaveAndReturnFavorite(request);

        assertEquals(user, favorite.getUser());
        assertEquals(game, favorite.getGame());
    }

    @Test
    void validateDtoShouldThrowWhenUserMissing() {
        FavoriteRequest request = new FavoriteRequest(1L, 2L);
        when(userService.get(1L)).thenReturn(Optional.empty());

        assertThrows(EntityCannotBeNull.class, () -> favoriteService.validateDtoSaveAndReturnFavorite(request));
        verifyNoInteractions(gameService);
    }

    @Test
    void validateDtoShouldThrowWhenGameMissing() {
        User user = User.builder().email("u@mail.com").username("u").password("x").build();
        FavoriteRequest request = new FavoriteRequest(1L, 2L);
        when(userService.get(1L)).thenReturn(Optional.of(user));
        when(gameService.get(2L)).thenReturn(Optional.empty());

        assertThrows(EntityCannotBeNull.class, () -> favoriteService.validateDtoSaveAndReturnFavorite(request));
    }

    @Test
    void saveGetAndDeleteShouldDelegateToRepository() {
        Favorite favorite = Favorite.builder().build();
        when(favoriteRepository.save(favorite)).thenReturn(favorite);
        when(favoriteRepository.findById(9L)).thenReturn(Optional.of(favorite));

        assertSame(favorite, favoriteService.save(favorite));
        assertTrue(favoriteService.get(9L).isPresent());
        favoriteService.delete(favorite);

        verify(favoriteRepository).save(favorite);
        verify(favoriteRepository).findById(9L);
        verify(favoriteRepository).delete(favorite);
    }

    @Test
    void getByUserAndGameShouldQueryByExample() {
        User user = User.builder().email("u@mail.com").username("u").password("x").build();
        Game game = Game.builder().name("Game").slug("game").averageRating(0.0).defaultRating(0.0).build();
        Favorite found = Favorite.builder().user(user).game(game).build();

        when(favoriteRepository.findOne(any())).thenReturn(Optional.of(found));

        Optional<Favorite> result = favoriteService.getByUserAndGame(user, game);

        assertTrue(result.isPresent());
        assertSame(found, result.get());

        ArgumentCaptor<Example<Favorite>> captor = ArgumentCaptor.forClass(Example.class);
        verify(favoriteRepository).findOne(captor.capture());
        Favorite probe = captor.getValue().getProbe();
        assertEquals(user, probe.getUser());
        assertEquals(game, probe.getGame());
    }
}
