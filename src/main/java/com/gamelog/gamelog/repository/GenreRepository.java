package com.gamelog.gamelog.repository;

import com.gamelog.gamelog.model.Genre;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenreRepository extends JpaRepository<Genre, Long> {
}
