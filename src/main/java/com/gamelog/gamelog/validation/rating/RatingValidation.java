package com.gamelog.gamelog.validation.rating;

import com.gamelog.gamelog.model.Rating;

public interface RatingValidation {

    void validateUniqueUserGame(Rating rating);
}
