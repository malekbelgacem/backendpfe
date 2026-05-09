package com.example.backendpfe.fraud;

import com.example.backendpfe.transaction.RiskLevel;
import com.example.backendpfe.transaction.ScoringStatus;
import com.example.backendpfe.transaction.Transaction;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "fraud_prediction_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudPredictionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Column(nullable = false)
    private Double fraudScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RiskLevel riskLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ScoringStatus scoringStatus;

    @Column(length = 1000)
    private String reason;

    @Column(length = 100)
    private String modelVersion;

    @Column(nullable = false)
    private Instant predictedAt;
}
