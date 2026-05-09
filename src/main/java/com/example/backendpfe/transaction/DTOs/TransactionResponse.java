package com.example.backendpfe.transaction.dto;

import com.example.backendpfe.transaction.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class TransactionResponse {

    private Long idTransaction;
    private String transactionRef;

    private Long sourceAccountId;
    private String sourceAccountNumber;

    private Long destinationAccountId;
    private String destinationAccountNumber;

    private Long initiatedByUserId;
    private String initiatedByUsername;

    private BigDecimal amount;
    private String currency;
    private TransactionChannel channel;
    private String merchant;
    private String merchantCategory;
    private String locationCountry;
    private String locationCity;
    private TransactionStatus status;
    private Instant timestamp;

    private Integer accountAgeDays;
    private Boolean isForeignTransaction;
    private Boolean cardPresent;
    private Integer hourOfDay;
    private Boolean isWeekend;
    private Integer velocity1h;
    private Integer velocity24h;
    private BigDecimal avgAmount30d;
    private BigDecimal amountToAvgRatio;
    private Integer failedLoginAttempts24h;
    private Boolean newDevice;
    private BigDecimal ipRiskScore;
    private BigDecimal distanceFromHomeKm;

    private BigDecimal fraudScore;
    private RiskLevel riskLevel;
    private ScoringStatus scoringStatus;
    private String scoringMetadata;
    private Instant scoredAt;

    private Instant createdAt;
    private Instant updatedAt;
    private TransactionType transactionType;
    private Long transferId;
}