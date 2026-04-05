package com.gamelog.gamelog.repository;

import com.gamelog.gamelog.model.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
}
