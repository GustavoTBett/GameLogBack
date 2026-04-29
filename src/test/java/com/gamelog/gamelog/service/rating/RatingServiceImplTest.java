package com.gamelog.gamelog.service.rating;

import com.gamelog.gamelog.controller.dto.RatingRequest;
import com.gamelog.gamelog.exception.EntityCannotBeNull;
import com.gamelog.gamelog.model.Game;
import com.gamelog.gamelog.model.Rating;
import com.gamelog.gamelog.model.User;
import com.gamelog.gamelog.repository.RatingRepository;
import com.gamelog.gamelog.service.game.GameService;
import com.gamelog.gamelog.service.user.UserService;
import com.gamelog.gamelog.validation.rating.RatingValidation;
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
class RatingServiceImplTest {

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private UserService userService;

    @Mock
    private GameService gameService;

    @Mock
    private RatingValidation ratingValidation;

    @InjectMocks
    private RatingServiceImpl ratingService;

    @Test
    void buildRatingShouldBuildRatingWhenUserAndGameExist() {
        User user = User.builder().email("u@mail.com").username("u").password("x").build();
        Game game = mock(Game.class);
        RatingRequest request = new RatingRequest(2L, 10, "great");

        when(userService.get(1L)).thenReturn(Optional.of(user));
        when(gameService.get(2L)).thenReturn(Optional.of(game));

        Rating rating = ratingService.buildRating(request, 1L);

        assertEquals(user, rating.getUser());
        assertEquals(game, rating.getGame());
        assertEquals(10, rating.getScore());
        assertEquals("great", rating.getReview());
    }

    @Test
    void buildRatingShouldThrowWhenUserMissing() {
        RatingRequest request = new RatingRequest(2L, 10, "great");
        when(userService.get(1L)).thenReturn(Optional.empty());

        assertThrows(EntityCannotBeNull.class, () -> ratingService.buildRating(request, 1L));
        verifyNoInteractions(gameService);
    }

    @Test
    void buildRatingShouldThrowWhenGameMissing() {
        User user = User.builder().email("u@mail.com").username("u").password("x").build();
        RatingRequest request = new RatingRequest(2L, 10, "great");
        when(userService.get(1L)).thenReturn(Optional.of(user));
        when(gameService.get(2L)).thenReturn(Optional.empty());

        assertThrows(EntityCannotBeNull.class, () -> ratingService.buildRating(request, 1L));
    }

    @Test
    void saveShouldPersistAndRecalculateAverage() {
        Game game = mock(Game.class);
        Rating rating = Rating.builder().score(4).game(game).build();
        when(game.getId()).thenReturn(7L);
        when(ratingRepository.save(rating)).thenReturn(rating);
        when(ratingRepository.findAverageScoreByGameId(7L)).thenReturn(4.3333);
        when(gameService.get(7L)).thenReturn(Optional.of(game));
        when(gameService.save(game)).thenReturn(game);

        doNothing().when(ratingValidation).validateUniqueUserGame(rating);

        assertSame(rating, ratingService.save(rating));

        verify(ratingRepository).save(rating);
        verify(ratingRepository).findAverageScoreByGameId(7L);
        verify(game).setAverageRating(4.33);
        verify(gameService).save(game);
    }

    @Test
    void saveGetAndDeleteShouldDelegateToRepository() {
        Game game = mock(Game.class);
        Rating rating = Rating.builder().score(4).game(game).build();
        when(game.getId()).thenReturn(9L);
        when(ratingRepository.save(rating)).thenReturn(rating);
        when(ratingRepository.findById(9L)).thenReturn(Optional.of(rating));
        when(ratingRepository.findAverageScoreByGameId(9L)).thenReturn(4.0);
        when(gameService.get(9L)).thenReturn(Optional.of(game));
        when(gameService.save(game)).thenReturn(game);

        doNothing().when(ratingValidation).validateUniqueUserGame(rating);

        assertSame(rating, ratingService.save(rating));
        assertTrue(ratingService.get(9L).isPresent());
        ratingService.delete(rating);

        verify(ratingRepository).save(rating);
        verify(ratingRepository).findById(9L);
        verify(ratingRepository).delete(rating);
    }

    @Test
    void getByUserAndGameShouldQueryByExample() {
        User user = User.builder().email("u@mail.com").username("u").password("x").build();
        Game game = mock(Game.class);
        Rating found = Rating.builder().user(user).game(game).score(3).build();

        when(ratingRepository.findOne(any())).thenReturn(Optional.of(found));

        Optional<Rating> result = ratingService.getByUserAndGame(user, game);

        assertTrue(result.isPresent());
        assertSame(found, result.get());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Example<Rating>> captor = (ArgumentCaptor<Example<Rating>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(Example.class);
        verify(ratingRepository).findOne(captor.capture());
        Rating probe = captor.getValue().getProbe();
        assertEquals(user, probe.getUser());
        assertEquals(game, probe.getGame());
    }
}
