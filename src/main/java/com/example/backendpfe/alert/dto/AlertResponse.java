package com.example.backendpfe.alert.dto;

import com.example.backendpfe.alert.AlertStatus;
import com.example.backendpfe.transaction.RiskLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class AlertResponse {
    private Long id;
    private Long transactionId;
    private String transactionRef;
    private RiskLevel riskLevel;
    private Double fraudScore;
    private String reason;
    private AlertStatus status;
    private Long assignedToUserId;
    private String assignedToUsername;
    private Instant createdAt;
    private Instant updatedAt;
}