package com.gamelog.gamelog.controller;

import com.gamelog.gamelog.model.EnumUser.UserRole;
import com.gamelog.gamelog.model.User;
import com.gamelog.gamelog.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest {

    private MockMvc mockMvc;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService)).build();
    }

    @Test
    void createShouldReturnCreated() throws Exception {
        User saved = mock(User.class);
        when(saved.getId()).thenReturn(10L);
        when(saved.getEmail()).thenReturn("user@mail.com");
        when(saved.getUsername()).thenReturn("player");
        when(saved.getRole()).thenReturn(UserRole.USER);

        when(userService.save(any(User.class))).thenReturn(saved);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@mail.com\",\"username\":\"player\",\"password\":\"secret123\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/users/10"))
                .andExpect(jsonPath("$.email").value("user@mail.com"));

        verify(userService).save(any(User.class));
    }

    @Test
    void getByIdShouldReturnNotFoundWhenMissing() throws Exception {
        when(userService.get(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/users/999").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateShouldReturnOkWhenUserExists() throws Exception {
        User existing = User.builder()
                .email("old@mail.com")
                .username("old")
                .password("oldpass")
                .role(UserRole.USER)
                .build();
        when(userService.get(1L)).thenReturn(Optional.of(existing));
        when(userService.save(existing)).thenReturn(existing);

        mockMvc.perform(put("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new@mail.com\",\"username\":\"new\",\"password\":\"newpass\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@mail.com"))
                .andExpect(jsonPath("$.username").value("new"));

        verify(userService).save(existing);
    }

    @Test
    void deleteShouldReturnNoContentWhenUserExists() throws Exception {
        User user = User.builder().email("user@mail.com").username("u").password("p").build();
        when(userService.get(1L)).thenReturn(Optional.of(user));

        mockMvc.perform(delete("/users/1"))
                .andExpect(status().isNoContent());

        verify(userService).delete(user);
    }
}
