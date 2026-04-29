package com.gamelog.gamelog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamelog.gamelog.config.security.AppUserPrincipal;
import com.gamelog.gamelog.controller.dto.RatingRequest;
import com.gamelog.gamelog.model.Rating;
import com.gamelog.gamelog.service.rating.RatingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RatingControllerTest {

    private MockMvc mockMvc;
    private RatingService ratingService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        ratingService = mock(RatingService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new RatingController(ratingService)).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void createShouldReturnForbiddenWithoutAuthentication() throws Exception {
        RatingRequest request = new RatingRequest(2L, 10, "great");

        mockMvc.perform(post("/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(ratingService);
    }

    @Test
    void createShouldReturnCreatedWhenOwnerAuthenticated() throws Exception {
        RatingRequest request = new RatingRequest(2L, 10, "great");
        Rating rating = mock(Rating.class);
        Rating saved = mock(Rating.class);
        when(saved.getId()).thenReturn(55L);

        when(ratingService.buildRating(any(RatingRequest.class), eq(1L))).thenReturn(rating);
        when(ratingService.save(rating)).thenReturn(saved);

        mockMvc.perform(post("/ratings")
                        .with(authentication(1L, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/ratings/55"));

        verify(ratingService).buildRating(any(RatingRequest.class), eq(1L));
        verify(ratingService).save(rating);
    }

    @Test
    void getByIdShouldReturnNotFoundWhenMissing() throws Exception {
        when(ratingService.get(404L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/ratings/404").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteShouldReturnNoContentWhenExists() throws Exception {
        Rating rating = mock(Rating.class);
        when(ratingService.get(1L)).thenReturn(Optional.of(rating));

        mockMvc.perform(delete("/ratings/1"))
                .andExpect(status().isNoContent());

        verify(ratingService).delete(rating);
    }

    private static RequestPostProcessor authentication(Long userId, String role) {
        return request -> {
            Authentication authentication = mock(Authentication.class);
            AppUserPrincipal principal = mock(AppUserPrincipal.class);
            when(principal.getId()).thenReturn(userId);
            when(authentication.getPrincipal()).thenReturn(principal);
            doReturn(List.of(new SimpleGrantedAuthority("ROLE_" + role))).when(authentication).getAuthorities();
            request.setUserPrincipal(authentication);
            return request;
        };
    }
}
