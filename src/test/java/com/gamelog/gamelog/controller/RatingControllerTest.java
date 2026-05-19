package com.gamelog.gamelog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamelog.gamelog.config.security.AppUserPrincipal;
import com.gamelog.gamelog.controller.dto.RatingRequest;
import com.gamelog.gamelog.model.Rating;
import com.gamelog.gamelog.model.User;
import com.gamelog.gamelog.service.rating.RatingService;
import com.gamelog.gamelog.service.ratingvote.RatingVoteService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RatingControllerTest {

    private MockMvc mockMvc;
    private RatingService ratingService;
    private RatingVoteService ratingVoteService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        ratingService = mock(RatingService.class);
        ratingVoteService = mock(RatingVoteService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new RatingController(ratingService, ratingVoteService)).build();
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
        User owner = mock(User.class);

        when(saved.getId()).thenReturn(55L);
        when(saved.getScore()).thenReturn(10);
        when(saved.getReview()).thenReturn("great");
        when(saved.getUser()).thenReturn(owner);
        when(owner.getId()).thenReturn(1L);
        when(owner.getUsername()).thenReturn("gustavo");

        when(ratingService.buildRating(any(RatingRequest.class), eq(1L))).thenReturn(rating);
        when(ratingService.save(rating)).thenReturn(saved);

        mockMvc.perform(post("/ratings")
                        .with(authentication(1L, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/ratings/55"))
                .andExpect(jsonPath("$.source").value("APP"))
                .andExpect(jsonPath("$.score").value(10))
                .andExpect(jsonPath("$.canEdit").value(true))
                .andExpect(jsonPath("$.canVote").value(false));

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
        User owner = mock(User.class);

        when(owner.getId()).thenReturn(1L);
        when(rating.getUser()).thenReturn(owner);
        when(ratingService.get(1L)).thenReturn(Optional.of(rating));

        mockMvc.perform(delete("/ratings/1").with(authentication(1L, "USER")))
                .andExpect(status().isNoContent());

        verify(ratingService).delete(rating);
    }

    @Test
    void updateShouldReturnForbiddenWhenUserIsNotOwner() throws Exception {
        RatingRequest request = new RatingRequest(2L, 9, "updated");
        Rating rating = mock(Rating.class);
        User owner = mock(User.class);

        when(owner.getId()).thenReturn(2L);
        when(rating.getUser()).thenReturn(owner);
        when(ratingService.get(55L)).thenReturn(Optional.of(rating));

        mockMvc.perform(put("/ratings/55")
                        .with(authentication(1L, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(ratingService, never()).save(any());
    }

    @Test
    void deleteShouldReturnForbiddenWhenUserIsNotOwner() throws Exception {
        Rating rating = mock(Rating.class);
        User owner = mock(User.class);

        when(owner.getId()).thenReturn(2L);
        when(rating.getUser()).thenReturn(owner);
        when(ratingService.get(55L)).thenReturn(Optional.of(rating));

        mockMvc.perform(delete("/ratings/55").with(authentication(1L, "USER")))
                .andExpect(status().isForbidden());

        verify(ratingService, never()).delete(any());
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
