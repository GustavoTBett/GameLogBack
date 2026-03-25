package com.gamelog.gamelog.service.rating;

import com.gamelog.gamelog.controller.dto.RatingRequest;
import com.gamelog.gamelog.exception.EntityCannotBeNull;
import com.gamelog.gamelog.model.Favorite;
import com.gamelog.gamelog.model.Game;
import com.gamelog.gamelog.model.Rating;
import com.gamelog.gamelog.model.User;
import com.gamelog.gamelog.repository.RatingRepository;
import com.gamelog.gamelog.service.game.GameService;
import com.gamelog.gamelog.service.user.UserService;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RatingServiceImpl implements RatingService {

    private final RatingRepository ratingRepository;
    private final UserService userService;
    private final GameService gameService;

    public RatingServiceImpl(RatingRepository ratingRepository, UserService userService, GameService gameService) {
        this.ratingRepository = ratingRepository;
        this.userService = userService;
        this.gameService = gameService;
    }

    @Override
    public Rating validateDtoSaveAndReturnRating(RatingRequest ratingRequest) {
        User user = userService.get(ratingRequest.userId())
                .orElseThrow(() -> new EntityCannotBeNull("User not found with id " + ratingRequest.userId()));
        Game game = gameService.get(ratingRequest.gameId())
                .orElseThrow(() -> new EntityCannotBeNull("Game not found with id " + ratingRequest.gameId()));

        Rating rating = Rating.builder()
                .user(user)
                .game(game)
                .score(ratingRequest.score())
                .review(ratingRequest.review())
                .build();

        return rating;
    }

    @Override
    public Rating save(Rating rating) {
        return ratingRepository.save(rating);
    }

    @Override
    public Optional<Rating> get(Long id) {
        return ratingRepository.findById(id);
    }

    @Override
    public void delete(Rating rating) {
        ratingRepository.delete(rating);
    }

    @Override
    public Optional<Rating> getByUserAndGame(User user, Game game) {
        Rating probe = new Rating();
        probe.setUser(user);
        probe.setGame(game);
        return ratingRepository.findOne(Example.of(probe));
    }
}
