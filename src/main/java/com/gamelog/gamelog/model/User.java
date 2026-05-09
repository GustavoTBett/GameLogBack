package com.gamelog.gamelog.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gamelog.gamelog.model.enums.UserRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@Table(name = "app_user")
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class User extends MasterEntityWAudit {

    @NotBlank
    @Email
    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @NotBlank
    @Column(name = "username", unique = true, nullable = false)
    private String username;

    @NotBlank
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "password", nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @Builder.Default
    private UserRole role = UserRole.USER;

    private String avatarUrl;

    private String bio;

    @Column(name = "google_sub", unique = true)
    private String googleSub;

    @Column(name = "google_email_verified", nullable = false)
    @Builder.Default
    private Boolean googleEmailVerified = false;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<UserPlatformMapping> platforms;

}
