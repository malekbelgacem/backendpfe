package com.example.backendpfe.transfer.dto;

import com.example.backendpfe.transfer.TransferStatus;
import com.example.backendpfe.transfer.TransferType;
import com.example.backendpfe.transaction.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class TransferResponse {
    private Long id;

    private Long senderAccountId;
    private String senderAccountNumber;

    private Long receiverAccountId;
    private String receiverAccountNumber;

    private BigDecimal amount;
    private String currency;
    private String reason;

    private TransferType type;
    private TransferStatus status;
    private TransactionType transactionType;

    private Instant createdAt;
    private Instant validatedAt;
    private String rejectionReason;
}