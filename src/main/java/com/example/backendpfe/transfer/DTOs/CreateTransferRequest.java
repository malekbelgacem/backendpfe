package com.example.backendpfe.transfer.dto;

import com.example.backendpfe.transfer.TransferType;
import com.example.backendpfe.transaction.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateTransferRequest {

    @NotNull
    private Long senderAccountId;

    @NotNull
    private Long receiverAccountId;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    @NotBlank
    private String currency;

    @NotBlank
    private String reason;

    @NotNull
    private TransferType type;

    @NotNull
    private TransactionType transactionType;
}