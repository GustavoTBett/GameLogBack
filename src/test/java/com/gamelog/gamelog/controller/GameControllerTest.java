package com.gamelog.gamelog.controller;

import com.gamelog.gamelog.controller.dto.GameSummaryResponse;
import com.gamelog.gamelog.model.EnumUser.GamePlatform;
import com.gamelog.gamelog.service.game.GameService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
class GameControllerTest {

    private MockMvc mockMvc;

    private GameService gameService;

        @BeforeEach
        void setUp() {
                gameService = mock(GameService.class);
                mockMvc = MockMvcBuilders.standaloneSetup(new GameController(gameService)).build();
        }

    @Test
    void exploreShouldReturnPagedResponse() throws Exception {
        GameSummaryResponse response = new GameSummaryResponse(
                1L,
                "Elden Ring",
                "elden-ring",
                "desc",
                "desc pt",
                "cover",
                9.5,
                LocalDate.of(2022, 2, 25),
                "FromSoftware",
                100L,
                List.of("RPG"),
                List.of(GamePlatform.PC)
        );

        Page<GameSummaryResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 12), 1);
        when(gameService.explore(0, 12, null, null, null)).thenReturn(page);

        mockMvc.perform(get("/games/explore")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Elden Ring"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(12));

        verify(gameService).explore(0, 12, null, null, null);
    }

    @Test
    void getByIdShouldReturnNotFoundWhenGameDoesNotExist() throws Exception {
        when(gameService.get(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/games/99")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void getBySlugShouldReturnOkWhenSummaryExists() throws Exception {
        when(gameService.getSummaryBySlug(eq("elden-ring"))).thenReturn(Optional.of(new com.gamelog.gamelog.controller.dto.GameDetailResponse(
                1L,
                "Elden Ring",
                "elden-ring",
                "desc",
                "desc pt",
                "cover",
                9.5,
                LocalDate.of(2022, 2, 25),
                "FromSoftware",
                100L,
                List.of("RPG"),
                List.of(GamePlatform.PC),
                List.of()
        )));

        mockMvc.perform(get("/games/slug/elden-ring")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("elden-ring"));
    }
}
