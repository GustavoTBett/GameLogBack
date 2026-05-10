package com.gamelog.gamelog.service.genre;

import com.gamelog.gamelog.model.Genre;
import com.gamelog.gamelog.repository.GenreRepository;
import com.gamelog.gamelog.validation.genre.GenreValidation;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GenreServiceImpl implements GenreService{

    private final GenreRepository genreRepository;
    private final GenreValidation genreValidation;

    public GenreServiceImpl(GenreRepository genreRepository, GenreValidation genreValidation) {
        this.genreRepository = genreRepository;
        this.genreValidation = genreValidation;
    }

    @Override
    public Genre save(Genre genre) {
        genreValidation.validateUniqueName(genre);
        return genreRepository.save(genre);
    }

    @Override
    public Optional<Genre> get(Long id) {
        return genreRepository.findById(id);
    }

    @Override
    public void delete(Genre genre) {
        genreRepository.delete(genre);
    }

    @Override
    public Optional<Genre> findByName(String name) {
        return genreRepository.findByName(name);
    }

    @Override
    public List<Genre> findAll() {
        return genreRepository.findAll();
    }


}
