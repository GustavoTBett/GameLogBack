package com.gamelog.gamelog.validation.rating;

import com.gamelog.gamelog.exception.EntityCannotBeNull;
import com.gamelog.gamelog.exception.rating.AlreadyExistRatingWithUserAndGame;
import com.gamelog.gamelog.model.Rating;
import com.gamelog.gamelog.service.rating.RatingService;
import com.gamelog.gamelog.validation.rating.RatingValidation;

import java.util.Objects;
import java.util.Optional;

public class RatingValidationImpl implements RatingValidation {

    private final RatingService ratingService;

    public RatingValidationImpl(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @Override
    public void validateUniqueUserGame(Rating rating) {
        if (rating == null) {
            throw new EntityCannotBeNull("Entiy cannot be null");
        }
        Optional<Rating> optionalRating = ratingService.getByUserAndGame(rating.getUser(), rating.getGame());
        if (optionalRating.isPresent() && !Objects.equals(optionalRating.get().getId(), rating.getId())) {
            throw new AlreadyExistRatingWithUserAndGame("Already exist a rating with this user and game");
        }
    }
}
