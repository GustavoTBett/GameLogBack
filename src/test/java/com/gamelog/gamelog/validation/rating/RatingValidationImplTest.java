package com.gamelog.gamelog.validation.rating;

import com.gamelog.gamelog.exception.EntityCannotBeNull;
import com.gamelog.gamelog.exception.rating.AlreadyExistRatingWithUserAndGame;
import com.gamelog.gamelog.model.Rating;
import com.gamelog.gamelog.repository.RatingRepository;
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
class RatingValidationImplTest {

    @Mock
    private RatingRepository ratingRepository;

    @InjectMocks
    private RatingValidationImpl ratingValidation;

    @Test
    void validateUniqueUserGameShouldThrowWhenRatingIsNull() {
        assertThrows(EntityCannotBeNull.class, () -> ratingValidation.validateUniqueUserGame(null));
    }

    @Test
    void validateUniqueUserGameShouldThrowWhenDifferentRatingExists() {
        Rating requestRating = org.mockito.Mockito.mock(Rating.class);
        Rating existingRating = org.mockito.Mockito.mock(Rating.class);

        com.gamelog.gamelog.model.User user = org.mockito.Mockito.mock(com.gamelog.gamelog.model.User.class);
        com.gamelog.gamelog.model.Game game = org.mockito.Mockito.mock(com.gamelog.gamelog.model.Game.class);
        when(user.getId()).thenReturn(1L);
        when(game.getId()).thenReturn(2L);
        when(requestRating.getUser()).thenReturn(user);
        when(requestRating.getGame()).thenReturn(game);
        when(requestRating.getId()).thenReturn(1L);
        when(existingRating.getId()).thenReturn(2L);
        when(ratingRepository.findFirstByUserIdAndGameId(1L, 2L))
                .thenReturn(Optional.of(existingRating));

        assertThrows(AlreadyExistRatingWithUserAndGame.class, () -> ratingValidation.validateUniqueUserGame(requestRating));
    }

    @Test
    void validateUniqueUserGameShouldPassWhenNoExistingRating() {
        Rating requestRating = org.mockito.Mockito.mock(Rating.class);
        com.gamelog.gamelog.model.User user = org.mockito.Mockito.mock(com.gamelog.gamelog.model.User.class);
        com.gamelog.gamelog.model.Game game = org.mockito.Mockito.mock(com.gamelog.gamelog.model.Game.class);
        when(user.getId()).thenReturn(1L);
        when(game.getId()).thenReturn(2L);
        when(requestRating.getUser()).thenReturn(user);
        when(requestRating.getGame()).thenReturn(game);
        when(ratingRepository.findFirstByUserIdAndGameId(1L, 2L))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> ratingValidation.validateUniqueUserGame(requestRating));
    }
}
