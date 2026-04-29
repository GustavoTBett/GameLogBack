package com.gamelog.gamelog.validation.rating;

import com.gamelog.gamelog.exception.EntityCannotBeNull;
import com.gamelog.gamelog.exception.rating.AlreadyExistRatingWithUserAndGame;
import com.gamelog.gamelog.model.Rating;
import com.gamelog.gamelog.repository.RatingRepository;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

@Component
public class RatingValidationImpl implements RatingValidation {

    private final RatingRepository ratingRepository;

    public RatingValidationImpl(RatingRepository ratingRepository) {
        this.ratingRepository = ratingRepository;
    }

    @Override
    public void validateUniqueUserGame(Rating rating) {
        if (rating == null) {
            throw new EntityCannotBeNull("Entiy cannot be null");
        }
        Optional<Rating> optionalRating = ratingRepository.findFirstByUserIdAndGameId(
                rating.getUser().getId(),
                rating.getGame().getId()
        );
        if (optionalRating.isPresent() && !Objects.equals(optionalRating.get().getId(), rating.getId())) {
            throw new AlreadyExistRatingWithUserAndGame("Already exist a rating with this user and game");
        }
    }
}
