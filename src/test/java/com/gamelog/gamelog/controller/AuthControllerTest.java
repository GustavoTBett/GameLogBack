package com.gamelog.gamelog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamelog.gamelog.config.security.AppUserPrincipal;
import com.gamelog.gamelog.service.auth.PasswordResetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private MockMvc mockMvc;
    private AuthenticationManager authenticationManager;
    private SecurityContextRepository securityContextRepository;
    private PasswordResetService passwordResetService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        authenticationManager = mock(AuthenticationManager.class);
        securityContextRepository = mock(SecurityContextRepository.class);
        passwordResetService = mock(PasswordResetService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new AuthController(authenticationManager, securityContextRepository, passwordResetService)
        ).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void loginShouldAuthenticateAndReturnAuthUserResponse() throws Exception {
        Authentication authentication = mock(Authentication.class);
        AppUserPrincipal principal = mock(AppUserPrincipal.class);

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(principal.getId()).thenReturn(7L);
        when(principal.getUsername()).thenReturn("player");
        when(principal.getEmail()).thenReturn("user@mail.com");
        when(principal.getRole()).thenReturn("USER");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"player\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.username").value("player"))
                .andExpect(jsonPath("$.email").value("user@mail.com"));

        verify(authenticationManager).authenticate(any());
        verify(securityContextRepository).saveContext(any(), any(), any());
    }

    @Test
    void meShouldReturnCurrentUser() throws Exception {
        mockMvc.perform(get("/auth/me")
                        .with(authentication(9L, "admin", "admin@mail.com", "ADMIN"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void forgotPasswordShouldReturnGenericMessage() throws Exception {
        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@mail.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        verify(passwordResetService).requestReset("user@mail.com");
    }

    @Test
    void resetPasswordShouldDelegateAndReturnSuccessMessage() throws Exception {
        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"abc123\",\"newPassword\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Senha redefinida com sucesso."));

        verify(passwordResetService).resetPassword("abc123", "123456");
    }

    @Test
    void logoutShouldReturnNoContent() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isNoContent());
    }

    private static RequestPostProcessor authentication(Long userId, String username, String email, String role) {
        return request -> {
            Authentication authentication = mock(Authentication.class);
            AppUserPrincipal principal = mock(AppUserPrincipal.class);
            when(authentication.getPrincipal()).thenReturn(principal);
            when(principal.getId()).thenReturn(userId);
            when(principal.getUsername()).thenReturn(username);
            when(principal.getEmail()).thenReturn(email);
            when(principal.getRole()).thenReturn(role);
            request.setUserPrincipal(authentication);
            return request;
        };
    }
}
