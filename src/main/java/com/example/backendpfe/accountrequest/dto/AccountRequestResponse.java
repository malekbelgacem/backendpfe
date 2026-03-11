package com.example.backendpfe.accountrequest.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class AccountRequestResponse {
    private Long idRequest;
    private String status;
    private Instant createdAt;
    private Instant decidedAt;
    private Long clientId;
    private String clientUsername;
    private String decisionNote;

    // ✅ nouveaux champs
    private String accountType;
    private String description;
    private String documentPath;
}