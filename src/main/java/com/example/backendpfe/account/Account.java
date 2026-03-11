package com.example.backendpfe.account;

import com.example.backendpfe.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "accounts")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idAccount;

    @Column(nullable = false, unique = true, length = 40)
    private String accountNumber;

    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(nullable = false)
    private Instant createdAt;


    // ✅ CLIENT (صاحب الحساب)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    // ✅ ANALYST المسؤول
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analyst_id", nullable = false)
    private User analyst;

    // ✅ AUDITOR المسؤول
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auditor_id", nullable = false)
    private User auditor;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (balance == null) balance = BigDecimal.ZERO;
        if (status == null) status = AccountStatus.ACTIVE;
        if (isDeleted == null) isDeleted = false;
    }
}