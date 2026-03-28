package com.gamelog.gamelog.service.rating;

import com.gamelog.gamelog.controller.dto.RatingRequest;
import com.gamelog.gamelog.model.Game;
import com.gamelog.gamelog.model.Rating;
import com.gamelog.gamelog.model.User;

import java.util.Optional;

public interface RatingService {

    Rating validateDtoSaveAndReturnRating(RatingRequest ratingRequest);

    Rating save(Rating rating);

    Optional<Rating> get(Long id);

    void delete(Rating rating);

    Optional<Rating> getByUserAndGame(User user, Game game);
}
