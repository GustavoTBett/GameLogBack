package com.gamelog.gamelog.service.rating;

import com.gamelog.gamelog.controller.dto.RatingRequest;
import com.gamelog.gamelog.exception.EntityCannotBeNull;
import com.gamelog.gamelog.model.Game;
import com.gamelog.gamelog.model.Rating;
import com.gamelog.gamelog.model.User;
import com.gamelog.gamelog.repository.RatingRepository;
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
class RatingServiceImplTest {

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private UserService userService;

    @Mock
    private GameService gameService;

    @InjectMocks
    private RatingServiceImpl ratingService;

    @Test
    void validateDtoShouldBuildRatingWhenUserAndGameExist() {
        User user = User.builder().email("u@mail.com").username("u").password("x").build();
        Game game = Game.builder().name("Game").slug("game").averageRating(0.0).defaultRating(0.0).build();
        RatingRequest request = new RatingRequest(1L, 2L, 5, "great");

        when(userService.get(1L)).thenReturn(Optional.of(user));
        when(gameService.get(2L)).thenReturn(Optional.of(game));

        Rating rating = ratingService.validateDtoSaveAndReturnRating(request);

        assertEquals(user, rating.getUser());
        assertEquals(game, rating.getGame());
        assertEquals(5, rating.getScore());
        assertEquals("great", rating.getReview());
    }

    @Test
    void validateDtoShouldThrowWhenUserMissing() {
        RatingRequest request = new RatingRequest(1L, 2L, 5, "great");
        when(userService.get(1L)).thenReturn(Optional.empty());

        assertThrows(EntityCannotBeNull.class, () -> ratingService.validateDtoSaveAndReturnRating(request));
        verifyNoInteractions(gameService);
    }

    @Test
    void validateDtoShouldThrowWhenGameMissing() {
        User user = User.builder().email("u@mail.com").username("u").password("x").build();
        RatingRequest request = new RatingRequest(1L, 2L, 5, "great");
        when(userService.get(1L)).thenReturn(Optional.of(user));
        when(gameService.get(2L)).thenReturn(Optional.empty());

        assertThrows(EntityCannotBeNull.class, () -> ratingService.validateDtoSaveAndReturnRating(request));
    }

    @Test
    void saveGetAndDeleteShouldDelegateToRepository() {
        Rating rating = Rating.builder().score(4).build();
        when(ratingRepository.save(rating)).thenReturn(rating);
        when(ratingRepository.findById(9L)).thenReturn(Optional.of(rating));

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
        Game game = Game.builder().name("Game").slug("game").averageRating(0.0).defaultRating(0.0).build();
        Rating found = Rating.builder().user(user).game(game).score(3).build();

        when(ratingRepository.findOne(any())).thenReturn(Optional.of(found));

        Optional<Rating> result = ratingService.getByUserAndGame(user, game);

        assertTrue(result.isPresent());
        assertSame(found, result.get());

        ArgumentCaptor<Example<Rating>> captor = ArgumentCaptor.forClass(Example.class);
        verify(ratingRepository).findOne(captor.capture());
        Rating probe = captor.getValue().getProbe();
        assertEquals(user, probe.getUser());
        assertEquals(game, probe.getGame());
    }
}
