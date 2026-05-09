package com.example.backendpfe.alert;

import com.example.backendpfe.alert.dto.AlertResponse;
import com.example.backendpfe.alert.dto.UpdateAlertStatusRequest;
import com.example.backendpfe.audit.AuditAction;
import com.example.backendpfe.audit.AuditLogService;
import com.example.backendpfe.transaction.Transaction;
import com.example.backendpfe.user.User;
import com.example.backendpfe.user.UserRepository;
import com.example.backendpfe.websocket.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;
    private final WebSocketNotificationService webSocketNotificationService;

    public void createAlertIfNeeded(Transaction transaction, Double fraudScore, String reason) {
        if (transaction == null || fraudScore == null || transaction.getRiskLevel() == null) {
            return;
        }

        if (alertRepository.existsByTransaction_IdTransaction(transaction.getIdTransaction())) {
            return;
        }

        User analyst = null;

        if (transaction.getSourceAccount() != null) {
            analyst = transaction.getSourceAccount().getAnalyst();
        }

        Alert alert = Alert.builder()
                .transaction(transaction)
                .riskLevel(transaction.getRiskLevel())
                .fraudScore(fraudScore)
                .reason(reason)
                .status(AlertStatus.OPEN)
                .assignedTo(analyst)
                .createdAt(Instant.now())
                .updatedAt(null)
                .build();

        Alert savedAlert = alertRepository.save(alert);
        webSocketNotificationService.sendAlertUpdate(toResponse(savedAlert));

        auditLogService.record(
                AuditAction.CREATE_FRAUD_ALERT,
                "ALERT",
                savedAlert.getId(),
                transaction.getInitiatedBy(),
                null,
                "Fraud alert created for transaction "
                        + transaction.getTransactionRef()
                        + " with score "
                        + fraudScore
                        + " and risk level "
                        + transaction.getRiskLevel()
        );
    }

    public Page<AlertResponse> getAll(Pageable pageable) {
        return alertRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toResponse);
    }

    public Page<AlertResponse> getByStatus(AlertStatus status, Pageable pageable) {
        return alertRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                .map(this::toResponse);
    }

    public AlertResponse getById(Long id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alert not found with id: " + id));
        return toResponse(alert);
    }

    public AlertResponse updateStatus(Long alertId, UpdateAlertStatusRequest request) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found with id: " + alertId));

        AlertStatus oldStatus = alert.getStatus();

        alert.setStatus(request.getStatus());
        alert.setUpdatedAt(Instant.now());

        Alert savedAlert = alertRepository.save(alert);
        webSocketNotificationService.sendAlertUpdate(toResponse(savedAlert));

        auditLogService.record(
                AuditAction.UPDATE_ALERT_STATUS,
                "ALERT",
                savedAlert.getId(),
                getCurrentUser(),
                null,
                "Alert status updated from "
                        + oldStatus
                        + " to "
                        + savedAlert.getStatus()
        );

        return toResponse(savedAlert);
    }

    private User getCurrentUser() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();

            return userRepository.findByUsernameAndIsDeletedFalse(username)
                    .orElse(null);

        } catch (Exception e) {
            return null;
        }
    }

    private AlertResponse toResponse(Alert alert) {
        return AlertResponse.builder()
                .id(alert.getId())
                .transactionId(alert.getTransaction().getIdTransaction())
                .transactionRef(alert.getTransaction().getTransactionRef())
                .riskLevel(alert.getRiskLevel())
                .fraudScore(alert.getFraudScore())
                .reason(alert.getReason())
                .status(alert.getStatus())
                .assignedToUserId(alert.getAssignedTo() != null ? alert.getAssignedTo().getIdUser() : null)
                .assignedToUsername(alert.getAssignedTo() != null ? alert.getAssignedTo().getUsername() : null)
                .createdAt(alert.getCreatedAt())
                .updatedAt(alert.getUpdatedAt())
                .build();
    }
    public Page<AlertResponse> getMyAlerts(String username, Pageable pageable) {
        return alertRepository
                .findByAssignedTo_UsernameOrderByCreatedAtDesc(username, pageable)
                .map(this::toResponse);
    }
}