package com.gamelog.gamelog.service.favorite;

import com.gamelog.gamelog.controller.dto.FavoriteRequest;
import com.gamelog.gamelog.exception.EntityCannotBeNull;
import com.gamelog.gamelog.model.Favorite;
import com.gamelog.gamelog.model.Game;
import com.gamelog.gamelog.model.User;
import com.gamelog.gamelog.repository.FavoriteRepository;
import com.gamelog.gamelog.service.game.GameService;
import com.gamelog.gamelog.service.user.UserService;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class FavoriteServiceImpl implements FavoriteService{

    private final FavoriteRepository favoriteRepository;
    private final UserService userService;
    private final GameService gameService;

    public FavoriteServiceImpl(FavoriteRepository favoriteRepository, UserService userService, GameService gameService) {
        this.favoriteRepository = favoriteRepository;
        this.userService = userService;
        this.gameService = gameService;
    }

    @Override
    public Favorite validateDtoSaveAndReturnFavorite(FavoriteRequest favoriteRequest) {
        User user = userService.get(favoriteRequest.userId())
                .orElseThrow(() -> new EntityCannotBeNull("User not found with id " + favoriteRequest.userId()));
        Game game = gameService.get(favoriteRequest.gameId())
                .orElseThrow(() -> new EntityCannotBeNull("Game not found with id " + favoriteRequest.gameId()));

        Favorite favorite = Favorite.builder()
                .user(user)
                .game(game)
                .build();

        return favorite;
    }

    @Override
    public Favorite save(Favorite favorite) {
        return favoriteRepository.save(favorite);
    }

    @Override
    public Optional<Favorite> get(Long id) {
        return favoriteRepository.findById(id);
    }

    @Override
    public void delete(Favorite favorite) {
        favoriteRepository.delete(favorite);
    }

    @Override
    public Optional<Favorite> getByUserAndGame(User user, Game game) {
        Favorite probe = new Favorite();
        probe.setUser(user);
        probe.setGame(game);
        return favoriteRepository.findOne(Example.of(probe));
    }
}
