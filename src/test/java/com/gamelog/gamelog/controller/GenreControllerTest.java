package com.gamelog.gamelog.controller;

import com.gamelog.gamelog.model.Genre;
import com.gamelog.gamelog.service.genre.GenreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GenreControllerTest {

    private MockMvc mockMvc;

    private GenreService genreService;

    @BeforeEach
    void setUp() {
        genreService = mock(GenreService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new GenreController(genreService)).build();
    }

    @Test
    void findAllShouldReturnGenres() throws Exception {
        when(genreService.findAll()).thenReturn(List.of(
                Genre.builder().id(1L).name("Action").build(),
                Genre.builder().id(2L).name("RPG").build()
        ));

        mockMvc.perform(get("/genres").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Action"))
                .andExpect(jsonPath("$[1].name").value("RPG"));
    }

    @Test
    void getByIdShouldReturnNotFoundWhenMissing() throws Exception {
        when(genreService.get(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/genres/999").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
