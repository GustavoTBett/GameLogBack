package com.gamelog.gamelog.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.io.Serializable;

@Data
@Entity
@Table(name = "game_genre")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameGenre implements Serializable {

    @EmbeddedId
    private GameGenreId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("gameId")
    @JoinColumn(name = "game_id", nullable = false)
    @NotNull(message = "Game is required")
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("genreId")
    @JoinColumn(name = "genre_id", nullable = false)
    @NotNull(message = "Genre is required")
    private Genre genre;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GameGenre that = (GameGenre) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}

