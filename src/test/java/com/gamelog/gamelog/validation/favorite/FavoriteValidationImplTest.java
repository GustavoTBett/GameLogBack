package com.gamelog.gamelog.validation.favorite;

import com.gamelog.gamelog.exception.EntityCannotBeNull;
import com.gamelog.gamelog.exception.favorite.AlreadyExistFavoriteWithUserAndGame;
import com.gamelog.gamelog.model.Favorite;
import com.gamelog.gamelog.repository.FavoriteRepository;
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
class FavoriteValidationImplTest {

    @Mock
    private FavoriteRepository favoriteRepository;

    @InjectMocks
    private FavoriteValidationImpl favoriteValidation;

    @Test
    void validateUniqueUserGameShouldThrowWhenFavoriteIsNull() {
        assertThrows(EntityCannotBeNull.class, () -> favoriteValidation.validateUniqueUserGame(null));
    }

    @Test
    void validateUniqueUserGameShouldThrowWhenDifferentFavoriteExists() {
        Favorite requestFavorite = org.mockito.Mockito.mock(Favorite.class);
        Favorite existingFavorite = org.mockito.Mockito.mock(Favorite.class);
        com.gamelog.gamelog.model.User user = org.mockito.Mockito.mock(com.gamelog.gamelog.model.User.class);
        com.gamelog.gamelog.model.Game game = org.mockito.Mockito.mock(com.gamelog.gamelog.model.Game.class);

        when(requestFavorite.getUser()).thenReturn(user);
        when(requestFavorite.getGame()).thenReturn(game);
        when(user.getId()).thenReturn(10L);
        when(game.getId()).thenReturn(20L);
        when(requestFavorite.getId()).thenReturn(1L);
        when(existingFavorite.getId()).thenReturn(2L);
        when(favoriteRepository.findFirstByUserIdAndGameId(10L, 20L))
                .thenReturn(Optional.of(existingFavorite));

        assertThrows(AlreadyExistFavoriteWithUserAndGame.class, () -> favoriteValidation.validateUniqueUserGame(requestFavorite));
    }

    @Test
    void validateUniqueUserGameShouldPassWhenNoExistingFavorite() {
        Favorite requestFavorite = org.mockito.Mockito.mock(Favorite.class);
        com.gamelog.gamelog.model.User user = org.mockito.Mockito.mock(com.gamelog.gamelog.model.User.class);
        com.gamelog.gamelog.model.Game game = org.mockito.Mockito.mock(com.gamelog.gamelog.model.Game.class);
        when(requestFavorite.getUser()).thenReturn(user);
        when(requestFavorite.getGame()).thenReturn(game);
        when(user.getId()).thenReturn(10L);
        when(game.getId()).thenReturn(20L);
        when(favoriteRepository.findFirstByUserIdAndGameId(10L, 20L))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> favoriteValidation.validateUniqueUserGame(requestFavorite));
    }
}
