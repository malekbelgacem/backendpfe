package com.example.backendpfe.transaction;

import com.example.backendpfe.account.Account;
import com.example.backendpfe.account.AccountRepository;
import com.example.backendpfe.alert.AlertService;
import com.example.backendpfe.audit.AuditAction;
import com.example.backendpfe.audit.AuditLogService;
import com.example.backendpfe.fraud.FraudDetectionClient;
import com.example.backendpfe.fraud.FraudPredictionHistory;
import com.example.backendpfe.fraud.FraudPredictionHistoryRepository;
import com.example.backendpfe.fraud.dto.FraudPredictionRequest;
import com.example.backendpfe.fraud.dto.FraudPredictionResponse;
import com.example.backendpfe.transaction.dto.CreateTransactionRequest;
import com.example.backendpfe.transaction.dto.TransactionResponse;
import com.example.backendpfe.transfer.Transfer;
import com.example.backendpfe.user.User;
import com.example.backendpfe.user.UserRepository;
import com.example.backendpfe.websocket.WebSocketNotificationService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final FraudDetectionClient fraudDetectionClient;
    private final FraudPredictionHistoryRepository fraudPredictionHistoryRepository;
    private final AlertService alertService;
    private final AuditLogService auditLogService;
    private final WebSocketNotificationService webSocketNotificationService;

    public TransactionResponse createTransaction(CreateTransactionRequest request) {
        Account sourceAccount = accountRepository.findById(request.getSourceAccountId())
                .orElseThrow(() -> new EntityNotFoundException("Source account not found"));

        Account destinationAccount = null;
        if (request.getDestinationAccountId() != null) {
            destinationAccount = accountRepository.findById(request.getDestinationAccountId())
                    .orElseThrow(() -> new EntityNotFoundException("Destination account not found"));
        }

        User initiatedBy = null;
        if (request.getInitiatedByUserId() != null) {
            initiatedBy = userRepository.findByIdUserAndIsDeletedFalse(request.getInitiatedByUserId())
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));
        }

        Transaction transaction = Transaction.builder()
                .transactionRef(generateTransactionRef())
                .sourceAccount(sourceAccount)
                .destinationAccount(destinationAccount)
                .initiatedBy(initiatedBy)
                .amount(request.getAmount())
                .currency(request.getCurrency().toUpperCase())
                .channel(request.getChannel())
                .transactionType(resolveTransactionType(destinationAccount))
                .merchant(request.getMerchant())
                .merchantCategory(request.getMerchantCategory())
                .locationCountry("TN")
                .locationCity("Unknown")
                .status(TransactionStatus.COMPLETED)
                .cardPresent(isCardPresentByChannel(request.getChannel()))
                .newDevice(false)
                .failedLoginAttempts24h(0)
                .ipRiskScore(BigDecimal.ZERO)
                .distanceFromHomeKm(BigDecimal.ZERO)
                .scoringStatus(ScoringStatus.NOT_REQUESTED)
                .riskLevel(RiskLevel.LOW)
                .fraudScore(BigDecimal.ZERO)
                .build();

        enrichTransactionFeatures(transaction, sourceAccount);

        Transaction saved = transactionRepository.save(transaction);

        auditLogService.record(
                AuditAction.CREATE_TRANSACTION,
                "TRANSACTION",
                saved.getIdTransaction(),
                initiatedBy,
                null,
                "Manual transaction created with reference " + saved.getTransactionRef()
        );

        TransactionResponse response = mapToResponse(saved);

// 🔥 envoyer transaction live vers dashboard analyste
        webSocketNotificationService.sendTransactionUpdate(response);

        return response;
    }

    public Transaction createFromTransfer(Transfer transfer) {
        Account sourceAccount = transfer.getAccountSender();
        Account destinationAccount = transfer.getAccountReceiver();
        User initiatedBy = sourceAccount.getClient();

        Transaction transaction = Transaction.builder()
                .transactionRef(generateTransactionRef())
                .sourceAccount(sourceAccount)
                .destinationAccount(destinationAccount)
                .initiatedBy(initiatedBy)
                .transfer(transfer)
                .amount(transfer.getAmount())
                .currency(transfer.getCurrency().toUpperCase())
                .channel(TransactionChannel.TRANSFER)
                .transactionType(transfer.getTransactionType())
                .merchant(null)
                .merchantCategory("BANK_TRANSFER")
                .locationCountry("TN")
                .locationCity("Unknown")
                .status(TransactionStatus.PENDING)
                .cardPresent(false)
                .newDevice(false)
                .failedLoginAttempts24h(0)
                .ipRiskScore(BigDecimal.ZERO)
                .distanceFromHomeKm(BigDecimal.ZERO)
                .scoringStatus(ScoringStatus.NOT_REQUESTED)
                .riskLevel(RiskLevel.LOW)
                .fraudScore(BigDecimal.ZERO)
                .build();

        enrichTransactionFeatures(transaction, sourceAccount);

        FraudPredictionRequest predictionRequest = FraudPredictionRequest.builder()
                .amount(transaction.getAmount().doubleValue())
                .hourOfDay(transaction.getHourOfDay())
                .isWeekend(Boolean.TRUE.equals(transaction.getIsWeekend()) ? 1 : 0)
                .velocity1h(transaction.getVelocity1h() != null ? transaction.getVelocity1h().doubleValue() : 0.0)
                .velocity24h(transaction.getVelocity24h() != null ? transaction.getVelocity24h().doubleValue() : 0.0)
                .amountToAvgRatio(transaction.getAmountToAvgRatio() != null ? transaction.getAmountToAvgRatio().doubleValue() : 1.0)
                .newDevice(Boolean.TRUE.equals(transaction.getNewDevice()) ? 1 : 0)
                .ipRiskScore(transaction.getIpRiskScore() != null ? transaction.getIpRiskScore().doubleValue() : 0.0)
                .distanceFromHomeKm(transaction.getDistanceFromHomeKm() != null ? transaction.getDistanceFromHomeKm().doubleValue() : 0.0)
                .currency(transaction.getCurrency())
                .channel(transaction.getChannel().name())
                .locationCountry(transaction.getLocationCountry() != null ? transaction.getLocationCountry() : "TN")
                .build();

        // Test Postman : force HIGH pour vérifier le blocage
        FraudPredictionResponse response = FraudPredictionResponse.builder()
                .fraudScore(0.95)
                .riskLevel("HIGH")
                .alert(true)
                .build();

        if (response != null) {
            transaction.setFraudScore(BigDecimal.valueOf(response.getFraudScore()));
            transaction.setRiskLevel(RiskLevel.valueOf(response.getRiskLevel()));
            transaction.setScoringStatus(ScoringStatus.SCORED);
            transaction.setScoredAt(Instant.now());

            if (transaction.getRiskLevel() == RiskLevel.HIGH
                    || transaction.getRiskLevel() == RiskLevel.MEDIUM) {
                transaction.setStatus(TransactionStatus.BLOCKED);
            } else {
                transaction.setStatus(TransactionStatus.COMPLETED);
            }

        } else {
            transaction.setScoringStatus(ScoringStatus.FAILED);
            transaction.setRiskLevel(RiskLevel.MEDIUM);
            transaction.setStatus(TransactionStatus.BLOCKED);
        }

        Transaction savedTransaction = transactionRepository.save(transaction);

        webSocketNotificationService.sendTransactionUpdate(mapToResponse(savedTransaction));

        auditLogService.record(
                AuditAction.CREATE_TRANSACTION_FROM_TRANSFER,
                "TRANSACTION",
                savedTransaction.getIdTransaction(),
                initiatedBy,
                null,
                "Transaction created automatically from transfer #" + transfer.getId()
                        + " with reference " + savedTransaction.getTransactionRef()
        );

        if (response != null) {
            savePredictionHistory(savedTransaction, response);

            auditLogService.record(
                    AuditAction.SCORE_TRANSACTION,
                    "TRANSACTION",
                    savedTransaction.getIdTransaction(),
                    initiatedBy,
                    null,
                    "Transaction scored by fraud model. Score="
                            + response.getFraudScore()
                            + ", riskLevel="
                            + response.getRiskLevel()
            );

            if (Boolean.TRUE.equals(response.getAlert())
                    || savedTransaction.getRiskLevel() == RiskLevel.HIGH
                    || savedTransaction.getRiskLevel() == RiskLevel.MEDIUM) {

                String alertReason = buildFraudReason(savedTransaction);

                alertService.createAlertIfNeeded(
                        savedTransaction,
                        response.getFraudScore(),
                        alertReason
                );

                auditLogService.record(
                        AuditAction.CREATE_FRAUD_ALERT,
                        "ALERT",
                        savedTransaction.getIdTransaction(),
                        initiatedBy,
                        null,
                        "Fraud alert created for transaction "
                                + savedTransaction.getTransactionRef()
                                + ". Reason: "
                                + alertReason
                );
            }
        }

        return savedTransaction;
    }

    public TransactionResponse getTransactionById(Long idTransaction) {
        Transaction transaction = transactionRepository.findById(idTransaction)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found"));
        return mapToResponse(transaction);
    }

    public Page<TransactionResponse> getTransactionsByAccount(Long accountId, int page, int size) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<Transaction> transactions = transactionRepository
                .findBySourceAccountOrDestinationAccount(account, account, pageable);

        return transactions.map(this::mapToResponse);
    }

    public Page<TransactionResponse> getTransactionsByUser(Long userId, int page, int size) {
        User user = userRepository.findByIdUserAndIsDeletedFalse(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<Transaction> transactions = transactionRepository.findByInitiatedBy(user, pageable);

        return transactions.map(this::mapToResponse);
    }

    public Page<TransactionResponse> getAllTransactions(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        return transactionRepository.findAll(pageable).map(this::mapToResponse);
    }

    public Page<TransactionResponse> searchTransactions(
            Long accountId,
            TransactionStatus status,
            TransactionChannel channel,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        if (!sortBy.equals("timestamp") && !sortBy.equals("amount") && !sortBy.equals("createdAt")) {
            sortBy = "timestamp";
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Specification<Transaction> specification = Specification
                .where(TransactionSpecification.hasAccountId(accountId))
                .and(TransactionSpecification.hasStatus(status))
                .and(TransactionSpecification.hasChannel(channel));

        return transactionRepository.findAll(specification, pageable).map(this::mapToResponse);
    }

    public Page<TransactionResponse> getClientTransactionHistory(String username, int page, int size) {
        User currentUser = userRepository.findByUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        List<Account> clientAccounts = accountRepository.findByClientAndIsDeletedFalse(currentUser);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));

        Page<Transaction> transactions = transactionRepository
                .findBySourceAccountInOrDestinationAccountIn(clientAccounts, clientAccounts, pageable);

        return transactions.map(this::mapToResponse);
    }

    public Page<TransactionResponse> getTransactionsForConnectedUser(String username, int page, int size) {
        User user = userRepository.findByUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        String role = user.getRole().getRoleName().name();

        if ("SUPER_ADMIN".equals(role)) {
            return transactionRepository.findAll(pageable).map(this::mapToResponse);
        }

        List<Account> accounts;

        if ("CLIENT".equals(role)) {
            accounts = accountRepository.findByClientAndIsDeletedFalse(user);
        } else if ("ANALYST".equals(role)) {
            accounts = accountRepository
                    .findAllByAnalyst_IdUserAndIsDeletedFalse(user.getIdUser(), Pageable.unpaged())
                    .getContent();
        } else if ("AUDITOR".equals(role)) {
            accounts = accountRepository
                    .findAllByAuditor_IdUserAndIsDeletedFalse(user.getIdUser(), Pageable.unpaged())
                    .getContent();
        } else {
            throw new IllegalStateException("Unauthorized role");
        }

        return transactionRepository
                .findBySourceAccountInOrDestinationAccountIn(accounts, accounts, pageable)
                .map(this::mapToResponse);
    }

    private void enrichTransactionFeatures(Transaction transaction, Account sourceAccount) {
        if (transaction.getTimestamp() == null) {
            transaction.setTimestamp(Instant.now());
        }

        int hour = transaction.getTimestamp().atZone(ZoneOffset.UTC).getHour();
        DayOfWeek day = transaction.getTimestamp().atZone(ZoneOffset.UTC).getDayOfWeek();

        transaction.setHourOfDay(hour);
        transaction.setIsWeekend(day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY);

        int velocity1h = transactionRepository.countBySourceAccountAndTimestampAfter(
                sourceAccount,
                Instant.now().minus(1, ChronoUnit.HOURS)
        );
        int velocity24h = transactionRepository.countBySourceAccountAndTimestampAfter(
                sourceAccount,
                Instant.now().minus(24, ChronoUnit.HOURS)
        );

        transaction.setVelocity1h(velocity1h + 1);
        transaction.setVelocity24h(velocity24h + 1);

        List<Transaction> last30Days = transactionRepository.findBySourceAccountAndTimestampAfter(
                sourceAccount,
                Instant.now().minus(30, ChronoUnit.DAYS)
        );

        if (last30Days.isEmpty()) {
            transaction.setAvgAmount30d(transaction.getAmount());
            transaction.setAmountToAvgRatio(BigDecimal.ONE);
        } else {
            BigDecimal sum = last30Days.stream()
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal avg = sum.divide(
                    BigDecimal.valueOf(last30Days.size()),
                    2,
                    RoundingMode.HALF_UP
            );

            transaction.setAvgAmount30d(avg);

            if (avg.compareTo(BigDecimal.ZERO) > 0) {
                transaction.setAmountToAvgRatio(
                        transaction.getAmount().divide(avg, 4, RoundingMode.HALF_UP)
                );
            } else {
                transaction.setAmountToAvgRatio(BigDecimal.ONE);
            }
        }

        transaction.setAccountAgeDays(0);

        if (transaction.getLocationCountry() != null) {
            transaction.setIsForeignTransaction(
                    !transaction.getLocationCountry().equalsIgnoreCase("TN")
                            && !transaction.getLocationCountry().equalsIgnoreCase("TUNISIA")
            );
        } else {
            transaction.setIsForeignTransaction(false);
        }
    }

    private boolean isCardPresentByChannel(TransactionChannel channel) {
        if (channel == null) return false;
        return channel == TransactionChannel.POS || channel == TransactionChannel.ATM;
    }

    private TransactionType resolveTransactionType(Account destinationAccount) {
        if (destinationAccount == null) {
            return TransactionType.EXTERNAL;
        }
        return TransactionType.INTERNAL;
    }

    private String generateTransactionRef() {
        return "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private TransactionResponse mapToResponse(Transaction t) {
        return TransactionResponse.builder()
                .idTransaction(t.getIdTransaction())
                .transactionRef(t.getTransactionRef())
                .sourceAccountId(t.getSourceAccount() != null ? t.getSourceAccount().getIdAccount() : null)
                .sourceAccountNumber(t.getSourceAccount() != null ? t.getSourceAccount().getAccountNumber() : null)
                .destinationAccountId(t.getDestinationAccount() != null ? t.getDestinationAccount().getIdAccount() : null)
                .destinationAccountNumber(t.getDestinationAccount() != null ? t.getDestinationAccount().getAccountNumber() : null)
                .initiatedByUserId(t.getInitiatedBy() != null ? t.getInitiatedBy().getIdUser() : null)
                .initiatedByUsername(t.getInitiatedBy() != null ? t.getInitiatedBy().getUsername() : null)
                .amount(t.getAmount())
                .currency(t.getCurrency())
                .channel(t.getChannel())
                .transactionType(t.getTransactionType())
                .merchant(t.getMerchant())
                .merchantCategory(t.getMerchantCategory())
                .locationCountry(t.getLocationCountry())
                .locationCity(t.getLocationCity())
                .status(t.getStatus())
                .timestamp(t.getTimestamp())
                .accountAgeDays(t.getAccountAgeDays())
                .isForeignTransaction(t.getIsForeignTransaction())
                .cardPresent(t.getCardPresent())
                .hourOfDay(t.getHourOfDay())
                .isWeekend(t.getIsWeekend())
                .velocity1h(t.getVelocity1h())
                .velocity24h(t.getVelocity24h())
                .avgAmount30d(t.getAvgAmount30d())
                .amountToAvgRatio(t.getAmountToAvgRatio())
                .failedLoginAttempts24h(t.getFailedLoginAttempts24h())
                .newDevice(t.getNewDevice())
                .ipRiskScore(t.getIpRiskScore())
                .distanceFromHomeKm(t.getDistanceFromHomeKm())
                .fraudScore(t.getFraudScore())
                .riskLevel(t.getRiskLevel())
                .scoringStatus(t.getScoringStatus())
                .scoringMetadata(t.getScoringMetadata())
                .scoredAt(t.getScoredAt())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .transferId(
                        t.getTransfer() != null
                                ? t.getTransfer().getId()
                                : null
                )
                .build();
    }

    private void savePredictionHistory(Transaction transaction, FraudPredictionResponse response) {
        String reason;

        if (Boolean.TRUE.equals(response.getAlert())) {
            reason = "Alert triggered by fraud detection model (HIGH RISK)";
        } else if ("MEDIUM".equalsIgnoreCase(response.getRiskLevel())) {
            reason = "Medium risk transaction detected";
        } else {
            reason = "Low risk transaction";
        }

        FraudPredictionHistory history = FraudPredictionHistory.builder()
                .transaction(transaction)
                .fraudScore(response.getFraudScore())
                .riskLevel(RiskLevel.valueOf(response.getRiskLevel()))
                .scoringStatus(ScoringStatus.SCORED)
                .reason(reason)
                .modelVersion("v1")
                .predictedAt(Instant.now())
                .build();

        fraudPredictionHistoryRepository.save(history);
    }
    private String buildFraudReason(Transaction t) {
        List<String> reasons = new ArrayList<>();

        if (Boolean.TRUE.equals(t.getNewDevice())) {
            reasons.add("New device detected");
        }

        if (Boolean.TRUE.equals(t.getIsForeignTransaction())) {
            reasons.add("Country changed (foreign transaction)");
        }

        if (t.getVelocity1h() != null && t.getVelocity1h() >= 5) {
            reasons.add("Multiple transfers in short time");
        }

        if (t.getAmountToAvgRatio() != null &&
                t.getAmountToAvgRatio().compareTo(BigDecimal.valueOf(3)) >= 0) {
            reasons.add("Amount much higher than usual");
        }

        if (t.getIpRiskScore() != null &&
                t.getIpRiskScore().compareTo(BigDecimal.valueOf(0.7)) >= 0) {
            reasons.add("High risk IP");
        }

        if (reasons.isEmpty()) {
            reasons.add("High fraud score detected by AI");
        }

        return String.join(", ", reasons);
    }
}