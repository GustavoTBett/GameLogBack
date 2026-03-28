package com.gamelog.gamelog.model;

import com.gamelog.gamelog.model.EnumUser.GamePlatform;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "user_platform", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "platform"})
})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPlatformMapping extends MasterEntityWAudit {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false)
    @NotNull(message = "Platform is required")
    private GamePlatform platform;
}
