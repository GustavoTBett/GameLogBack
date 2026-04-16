package com.gamelog.gamelog.service.genre;

import com.gamelog.gamelog.model.Genre;
import com.gamelog.gamelog.repository.GenreRepository;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GenreServiceImpl implements GenreService{

    private final GenreRepository genreRepository;

    public GenreServiceImpl(GenreRepository genreRepository) {
        this.genreRepository = genreRepository;
    }

    @Override
    public Genre save(Genre genre) {
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
        Genre example = new Genre();
        example.setName(name);
        return genreRepository.findOne(Example.of(example));
    }

    @Override
    public List<Genre> findAll() {
        return genreRepository.findAll();
    }


}
