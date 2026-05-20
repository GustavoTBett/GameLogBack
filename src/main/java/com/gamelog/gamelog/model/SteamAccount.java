package com.gamelog.gamelog.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "steam_account", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id"})
}, indexes = {
        @Index(name = "idx_steam_account_steam_id", columnList = "steam_id")
})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SteamAccount extends MasterEntityWAudit {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;

    @Column(name = "steam_id", nullable = false, length = 32)
    @NotNull(message = "steamId is required")
    private String steamId;

    @Column(name = "profile_url")
    private String profileUrl;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @Column(name = "synced", nullable = false)
    @Builder.Default
    private Boolean synced = false;
}
