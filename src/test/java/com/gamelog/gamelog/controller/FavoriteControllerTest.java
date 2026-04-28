package com.gamelog.gamelog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamelog.gamelog.config.security.AppUserPrincipal;
import com.gamelog.gamelog.controller.dto.FavoriteRequest;
import com.gamelog.gamelog.model.Favorite;
import com.gamelog.gamelog.service.favorite.FavoriteService;
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

class FavoriteControllerTest {

    private MockMvc mockMvc;
    private FavoriteService favoriteService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        favoriteService = mock(FavoriteService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new FavoriteController(favoriteService)).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void createShouldReturnForbiddenWithoutAuthentication() throws Exception {
        FavoriteRequest request = new FavoriteRequest(1L, 2L);

        mockMvc.perform(post("/favorites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(favoriteService);
    }

    @Test
    void createShouldReturnCreatedWhenOwnerAuthenticated() throws Exception {
        FavoriteRequest request = new FavoriteRequest(1L, 2L);
        Favorite favorite = mock(Favorite.class);
        Favorite saved = mock(Favorite.class);
        when(saved.getId()).thenReturn(12L);

        when(favoriteService.validateDtoSaveAndReturnFavorite(any(FavoriteRequest.class))).thenReturn(favorite);
        when(favoriteService.save(favorite)).thenReturn(saved);

        mockMvc.perform(post("/favorites")
                        .with(authentication(1L, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/favorites/12"));

        verify(favoriteService).validateDtoSaveAndReturnFavorite(any(FavoriteRequest.class));
        verify(favoriteService).save(favorite);
    }

    @Test
    void createShouldAllowAdminActingForAnotherUser() throws Exception {
        FavoriteRequest request = new FavoriteRequest(99L, 2L);
        Favorite favorite = mock(Favorite.class);
        Favorite saved = mock(Favorite.class);
        when(saved.getId()).thenReturn(13L);

        when(favoriteService.validateDtoSaveAndReturnFavorite(any(FavoriteRequest.class))).thenReturn(favorite);
        when(favoriteService.save(favorite)).thenReturn(saved);

        mockMvc.perform(post("/favorites")
                        .with(authentication(1L, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void getByIdShouldReturnNotFoundWhenMissing() throws Exception {
        when(favoriteService.get(404L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/favorites/404").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteShouldReturnNoContentWhenExists() throws Exception {
        Favorite favorite = mock(Favorite.class);
        when(favoriteService.get(1L)).thenReturn(Optional.of(favorite));

        mockMvc.perform(delete("/favorites/1"))
                .andExpect(status().isNoContent());

        verify(favoriteService).delete(favorite);
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
