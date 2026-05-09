package com.example.backendpfe.casemanagement.dto;

import com.example.backendpfe.casemanagement.CaseDecision;
import com.example.backendpfe.casemanagement.CaseStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FraudCaseResponse {

    private Long id;
    private String caseReference;

    private Long alertId;
    private String alertStatus;

    private Long transactionId;
    private String transactionRef;

    private Long ownerId;
    private String ownerUsername;

    private CaseStatus status;
    private CaseDecision finalDecision;

    private String title;
    private String description;
    private String justification;

    private Instant openedAt;
    private Instant closedAt;
}