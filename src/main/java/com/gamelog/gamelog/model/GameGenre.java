package com.gamelog.gamelog.model;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "game_genre")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameGenre extends MasterEntity{

    @ManyToOne
    @JoinColumn(name = "game_id", nullable = false)
    @NotNull(message = "Game is required")
    private Game game;

    @ManyToOne
    @JoinColumn(name = "genre_id", nullable = false)
    @NotNull(message = "Genre is required")
    private Genre genre;
}
