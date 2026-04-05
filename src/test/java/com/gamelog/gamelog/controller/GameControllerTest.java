package com.gamelog.gamelog.controller;

import com.gamelog.gamelog.service.favorite.FavoriteService;
import com.gamelog.gamelog.service.game.GameService;
import com.gamelog.gamelog.service.gameGenre.GameGenreService;
import com.gamelog.gamelog.service.genre.GenreService;
import com.gamelog.gamelog.service.rating.RatingService;
import com.gamelog.gamelog.service.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GameControllerTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(GameService.class, () -> mock(GameService.class))
            .withBean(UserService.class, () -> mock(UserService.class))
            .withBean(GenreService.class, () -> mock(GenreService.class))
            .withBean(FavoriteService.class, () -> mock(FavoriteService.class))
            .withBean(RatingService.class, () -> mock(RatingService.class))
            .withBean(GameGenreService.class, () -> mock(GameGenreService.class))
            .withBean(GameController.class)
            .withBean(UserController.class)
            .withBean(GenreController.class)
            .withBean(FavoriteController.class)
            .withBean(RatingController.class)
            .withBean(GameGenreController.class);

    @Test
    void shouldLoadGameControllerInContext() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(GameController.class);
            assertThat(context).hasSingleBean(UserController.class);
            assertThat(context).hasSingleBean(GenreController.class);
            assertThat(context).hasSingleBean(FavoriteController.class);
            assertThat(context).hasSingleBean(RatingController.class);
            assertThat(context).hasSingleBean(GameGenreController.class);
        });
    }
}
