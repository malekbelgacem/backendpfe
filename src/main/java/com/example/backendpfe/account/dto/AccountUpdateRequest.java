package com.example.backendpfe.account.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AccountUpdateRequest {
    private String accountNumber;
    private BigDecimal balance;
    private String status;   // ACTIVE / BLOCKED
    private Long clientId;
    private Long analystId;
    private Long auditorId;
}