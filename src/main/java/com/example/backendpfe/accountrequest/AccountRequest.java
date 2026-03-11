package com.example.backendpfe.accountrequest;

import com.example.backendpfe.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "account_requests")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AccountRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idRequest;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AccountRequestStatus status = AccountRequestStatus.PENDING;

    // شكون طلب الحساب (Client)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant decidedAt;

    private String decisionNote; // علاش accepted / rejected
    @Column(nullable = false, length = 20)
    private String accountType; // DEBIT / CREDIT

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(length = 500)
    private String documentPath;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = AccountRequestStatus.PENDING;
    }
}