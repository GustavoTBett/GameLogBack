package com.gamelog.gamelog.model;

import com.gamelog.gamelog.model.EnumUser.GamePlatform;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "game_platform", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"game_id", "platform"})
})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GamePlatformMapping extends MasterEntityWAudit {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    @NotNull(message = "Game is required")
    private Game game;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false)
    @NotNull(message = "Platform is required")
    private GamePlatform platform;
}
