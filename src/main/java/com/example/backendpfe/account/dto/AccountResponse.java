package com.example.backendpfe.account.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AccountResponse {
    private Long idAccount;
    private String accountNumber;
    private BigDecimal balance;
    private String status;
    private Instant createdAt;

    private Long clientId;
    private String clientUsername;

    private Long analystId;
    private String analystUsername;

    private Long auditorId;
    private String auditorUsername;
}