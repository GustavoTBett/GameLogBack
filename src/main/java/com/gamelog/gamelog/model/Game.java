package com.gamelog.gamelog.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@Table(name = "game")
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Game extends MasterEntityWAudit {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Slug is required")
    private String slug;

    private String description;

    private LocalDate releaseDate;

    private String developer;

    private String publiser;

    private String platform;

    private String cover_url;

    @Column(precision = 3, nullable = false)
    @NotNull(message = "Average rating is required")
    private Double averageRating;

    @Column(precision = 3, nullable = false)
    @NotNull(message = "Default rating is required")
    private Double defaultRating;

}
