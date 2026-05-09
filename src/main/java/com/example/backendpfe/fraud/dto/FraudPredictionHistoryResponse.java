package com.example.backendpfe.fraud.dto;



import com.example.backendpfe.transaction.RiskLevel;
import com.example.backendpfe.transaction.ScoringStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class FraudPredictionHistoryResponse {

    private Long id;
    private Long transactionId;
    private String transactionRef;
    private Double fraudScore;
    private RiskLevel riskLevel;
    private ScoringStatus scoringStatus;
    private String reason;
    private String modelVersion;
    private Instant predictedAt;
}