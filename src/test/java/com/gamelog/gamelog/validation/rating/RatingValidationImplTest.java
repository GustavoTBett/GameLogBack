package com.gamelog.gamelog.validation.rating;

import com.gamelog.gamelog.exception.EntityCannotBeNull;
import com.gamelog.gamelog.exception.rating.AlreadyExistRatingWithUserAndGame;
import com.gamelog.gamelog.model.Rating;
import com.gamelog.gamelog.service.rating.RatingService;
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
    private RatingService ratingService;

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

        when(requestRating.getUser()).thenReturn(org.mockito.Mockito.mock(com.gamelog.gamelog.model.User.class));
        when(requestRating.getGame()).thenReturn(org.mockito.Mockito.mock(com.gamelog.gamelog.model.Game.class));
        when(requestRating.getId()).thenReturn(1L);
        when(existingRating.getId()).thenReturn(2L);
        when(ratingService.getByUserAndGame(requestRating.getUser(), requestRating.getGame()))
                .thenReturn(Optional.of(existingRating));

        assertThrows(AlreadyExistRatingWithUserAndGame.class, () -> ratingValidation.validateUniqueUserGame(requestRating));
    }

    @Test
    void validateUniqueUserGameShouldPassWhenNoExistingRating() {
        Rating requestRating = org.mockito.Mockito.mock(Rating.class);
        when(requestRating.getUser()).thenReturn(org.mockito.Mockito.mock(com.gamelog.gamelog.model.User.class));
        when(requestRating.getGame()).thenReturn(org.mockito.Mockito.mock(com.gamelog.gamelog.model.Game.class));
        when(ratingService.getByUserAndGame(requestRating.getUser(), requestRating.getGame()))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> ratingValidation.validateUniqueUserGame(requestRating));
    }
}
