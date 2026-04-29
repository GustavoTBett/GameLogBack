package com.gamelog.gamelog.service.rating;

import com.gamelog.gamelog.controller.dto.RatingRequest;
import com.gamelog.gamelog.exception.EntityCannotBeNull;
import com.gamelog.gamelog.model.Game;
import com.gamelog.gamelog.model.Rating;
import com.gamelog.gamelog.model.User;
import com.gamelog.gamelog.repository.RatingRepository;
import com.gamelog.gamelog.validation.rating.RatingValidation;
import com.gamelog.gamelog.service.game.GameService;
import com.gamelog.gamelog.service.user.UserService;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Service
public class RatingServiceImpl implements RatingService {

    private final RatingRepository ratingRepository;
    private final UserService userService;
    private final GameService gameService;
    private final RatingValidation ratingValidation;

    public RatingServiceImpl(
            RatingRepository ratingRepository,
            UserService userService,
            GameService gameService,
            RatingValidation ratingValidation
    ) {
        this.ratingRepository = ratingRepository;
        this.userService = userService;
        this.gameService = gameService;
        this.ratingValidation = ratingValidation;
    }

    @Override
    public Rating buildRating(RatingRequest ratingRequest, Long userId) {
        User user = userService.get(userId)
                .orElseThrow(() -> new EntityCannotBeNull("User not found with id " + userId));
        Game game = gameService.get(ratingRequest.gameId())
                .orElseThrow(() -> new EntityCannotBeNull("Game not found with id " + ratingRequest.gameId()));

        return Rating.builder()
                .user(user)
                .game(game)
                .score(ratingRequest.score())
                .review(ratingRequest.review())
                .build();
    }

    @Override
    @Transactional
    public Rating save(Rating rating) {
        ratingValidation.validateUniqueUserGame(rating);

        Rating savedRating = ratingRepository.save(rating);
        recalculateGameAverage(savedRating.getGame().getId());

        return savedRating;
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

    private void recalculateGameAverage(Long gameId) {
        Game game = gameService.get(gameId)
                .orElseThrow(() -> new EntityCannotBeNull("Game not found with id " + gameId));

        Double averageScore = ratingRepository.findAverageScoreByGameId(gameId);
        double resolvedAverage = averageScore != null ? averageScore : 0.0;

        game.setAverageRating(roundToTwoDecimals(resolvedAverage));
        gameService.save(game);
    }

    private double roundToTwoDecimals(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
