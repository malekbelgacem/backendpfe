package com.example.backendpfe.user;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idUser;

    @Column(nullable = false, unique = true, length = 80)
    private String username;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(nullable = false)
    private String password;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isDeleted = false;

    @Column(length = 255)
    private String adresse;

    private Double latitude;
    private Double longitude;

    private Instant lastLogin;
    private Instant createdAt;
    @Column(name = "refresh_token")
    private String refreshToken;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (isActive == null) isActive = true;
        if (isDeleted == null) isDeleted = false;
    }
}