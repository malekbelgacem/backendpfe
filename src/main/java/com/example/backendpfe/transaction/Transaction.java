package com.example.backendpfe.transaction;

import com.example.backendpfe.account.Account;
import com.example.backendpfe.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import com.example.backendpfe.transfer.Transfer;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idTransaction;

    @Column(nullable = false, unique = true, length = 50)
    private String transactionRef;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_id")
    private Transfer transfer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_account_id", nullable = false)
    private Account sourceAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_account_id")
    private Account destinationAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiated_by_user_id")
    private User initiatedBy;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType transactionType;

    @Column(length = 150)
    private String merchant;

    @Column(length = 100)
    private String merchantCategory;

    @Column(length = 100)
    private String locationCountry;

    @Column(length = 100)
    private String locationCity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Column(nullable = false)
    private Instant timestamp;

    // ===== Features utiles pour IA =====

    private Integer accountAgeDays;

    @Builder.Default
    private Boolean isForeignTransaction = false;

    @Builder.Default
    private Boolean cardPresent = false;

    private Integer hourOfDay;

    @Builder.Default
    private Boolean isWeekend = false;

    private Integer velocity1h;
    private Integer velocity24h;

    @Column(precision = 19, scale = 2)
    private BigDecimal avgAmount30d;

    @Column(precision = 19, scale = 4)
    private BigDecimal amountToAvgRatio;

    private Integer failedLoginAttempts24h;

    @Builder.Default
    private Boolean newDevice = false;

    @Column(precision = 10, scale = 4)
    private BigDecimal ipRiskScore;

    @Column(precision = 10, scale = 2)
    private BigDecimal distanceFromHomeKm;

    // ===== Partie scoring IA =====

    @Column(precision = 10, scale = 4)
    private BigDecimal fraudScore;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RiskLevel riskLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ScoringStatus scoringStatus = ScoringStatus.NOT_REQUESTED;

    @Column(columnDefinition = "TEXT")
    private String scoringMetadata;

    private Instant scoredAt;

    // ===== Audit =====

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;

        if (this.timestamp == null) {
            this.timestamp = now;
        }
        if (this.status == null) {
            this.status = TransactionStatus.PENDING;
        }
        if (this.scoringStatus == null) {
            this.scoringStatus = ScoringStatus.NOT_REQUESTED;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}