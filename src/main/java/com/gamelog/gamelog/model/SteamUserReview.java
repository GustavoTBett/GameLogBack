package com.gamelog.gamelog.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "steam_user_review", indexes = {
        @Index(name = "idx_sur_appid", columnList = "app_id"),
        @Index(name = "idx_sur_account", columnList = "steam_account_id")
})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SteamUserReview extends MasterEntityWAudit {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "steam_account_id", nullable = false)
    @NotNull
    private SteamAccount steamAccount;

    @Column(name = "app_id", nullable = false)
    private Long appId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id")
    private Game game;

    @Column(name = "review_text", columnDefinition = "TEXT")
    private String reviewText;

    @Column(name = "language")
    private String language;

    @Column(name = "recommended")
    private Boolean recommended;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "imported_at")
    private Instant importedAt;
}
