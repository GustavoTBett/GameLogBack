package com.gamelog.gamelog.service.genre;

import com.gamelog.gamelog.model.Genre;

import java.util.List;
import java.util.Optional;

public interface GenreService {

    Genre save(Genre genre);

    Optional<Genre> get(Long id);

    void delete(Genre genre);

    Optional<Genre> findByName(String name);

    List<Genre> findAll();
}
