package com.gamelog.gamelog.model;

import com.gamelog.gamelog.validation.genre.GenreValidationImpl;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Data
@Table(name = "genre")
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(GenreValidationImpl.class)
public class Genre implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    private String name;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Genre genre = (Genre) o;
        return Objects.equals(id, genre.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
