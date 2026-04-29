package com.gamelog.gamelog.model;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@Entity
@Table(name = "rating")
@NoArgsConstructor
@AllArgsConstructor
public class Rating extends MasterEntityWAudit{

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;

    @ManyToOne
    @JoinColumn(name = "game_id", nullable = false)
    @NotNull(message = "Game is required")
    private Game game;

    @Min(value = 1L, message = "O valor minimo é 1")
    @Max(value = 10L, message = "O valor máximo é 10")
    @NotNull(message = "A nota é obrigatória")
    private Integer score;

    private String review;
}
