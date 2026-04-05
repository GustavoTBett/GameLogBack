package com.gamelog.gamelog.repository;

import com.gamelog.gamelog.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RatingRepository extends JpaRepository<Rating, Long> {
}
