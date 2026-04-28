package com.gamelog.gamelog.model;

import com.gamelog.gamelog.model.EnumUser.ImageSource;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Set;

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

    @Column(name = "description_pt_br")
    private String descriptionPtBr;

    private LocalDate releaseDate;

    private String developer;

    @Column(name = "publisher")
    private String publisher;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<GamePlatformMapping> platforms;

    @Column(name = "cover_url")
    private String cover_url;

    @Column(name = "rawg_image_url")
    private String rawgImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "image_source", nullable = false)
    private ImageSource imageSource;

    @Column(name = "image_last_checked_at")
    private OffsetDateTime imageLastCheckedAt;

    @Column(precision = 3, nullable = false)
    @NotNull(message = "Average rating is required")
    private Double averageRating;

    @Column(precision = 3, nullable = false)
    @NotNull(message = "Default rating is required")
    private Double defaultRating;

}
