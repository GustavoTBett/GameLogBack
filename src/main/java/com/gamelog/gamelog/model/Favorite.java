package com.gamelog.gamelog.model;

import com.gamelog.gamelog.validation.favorite.FavoriteValidationImpl;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@Table(name = "favorites")
@Entity
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(FavoriteValidationImpl.class)
public class Favorite extends MasterEntityWAudit {

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;

    @ManyToOne
    @JoinColumn(name = "game_id", nullable = false)
    @NotNull(message = "Game is required")
    private Game game;
}
