package com.example.backendpfe.transaction.dto;

import com.example.backendpfe.transaction.TransactionChannel;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateTransactionRequest {

    @NotNull
    private Long sourceAccountId;

    private Long destinationAccountId;

    private Long initiatedByUserId;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    @NotBlank
    private String currency;

    @NotNull
    private TransactionChannel channel;

    private String merchant;
    private String merchantCategory;
}