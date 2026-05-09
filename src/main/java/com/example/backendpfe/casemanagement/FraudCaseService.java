package com.example.backendpfe.casemanagement;

import com.example.backendpfe.alert.Alert;
import com.example.backendpfe.alert.AlertRepository;
import com.example.backendpfe.alert.AlertStatus;
import com.example.backendpfe.audit.AuditAction;
import com.example.backendpfe.audit.AuditLogService;
import com.example.backendpfe.casemanagement.dto.CreateCaseRequest;
import com.example.backendpfe.casemanagement.dto.FraudCaseResponse;
import com.example.backendpfe.casemanagement.dto.ResolveCaseRequest;
import com.example.backendpfe.user.User;
import com.example.backendpfe.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class FraudCaseService {

    private final FraudCaseRepository fraudCaseRepository;
    private final AlertRepository alertRepository;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    // ✅ CREATE CASE FROM ALERT (avec username)
    public FraudCaseResponse createFromAlert(Long alertId, CreateCaseRequest request, String username) {

        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found with id: " + alertId));

        if (fraudCaseRepository.existsByAlert_Id(alertId)) {
            throw new RuntimeException("Case already exists for alert id: " + alertId);
        }

        // 🔥 récupérer analyst connecté
        User analyst = userRepository.findByUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new RuntimeException("Analyst not found: " + username));

        // 🔥 créer case lié à analyst
        FraudCase fraudCase = FraudCase.builder()
                .alert(alert)
                .owner(analyst)
                .status(CaseStatus.OPEN)
                .justification(request.getJustification())
                .openedAt(Instant.now())
                .build();

        // 🔥 update alerte
        alert.setStatus(AlertStatus.IN_REVIEW);
        alertRepository.save(alert);

        FraudCase savedCase = fraudCaseRepository.save(fraudCase);

        // 🔥 audit log
        auditLogService.record(
                AuditAction.CREATE_CASE,
                "CASE",
                savedCase.getId(),
                analyst,
                null,
                "Fraud case opened from alert #" + alertId
                        + " linked to transaction "
                        + (alert.getTransaction() != null
                        ? alert.getTransaction().getTransactionRef()
                        : "N/A")
        );

        return toResponse(savedCase);
    }

    // ✅ GET ALL
    public Page<FraudCaseResponse> getAll(Pageable pageable) {
        return fraudCaseRepository.findAllByOrderByOpenedAtDesc(pageable)
                .map(this::toResponse);
    }

    // ✅ GET BY ID
    public FraudCaseResponse getById(Long id) {
        FraudCase fraudCase = fraudCaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Case not found with id: " + id));

        return toResponse(fraudCase);
    }

    // ✅ GET MY CASES
    public Page<FraudCaseResponse> getMyCases(String username, Pageable pageable) {
        return fraudCaseRepository
                .findByOwner_UsernameOrderByOpenedAtDesc(username, pageable)
                .map(this::toResponse);
    }

    // ✅ FILTER BY STATUS
    public Page<FraudCaseResponse> getByStatus(CaseStatus status, Pageable pageable) {
        return fraudCaseRepository.findByStatusOrderByOpenedAtDesc(status, pageable)
                .map(this::toResponse);
    }

    // ✅ RESOLVE CASE
    public FraudCaseResponse resolveCase(Long caseId, ResolveCaseRequest request) {

        FraudCase fraudCase = fraudCaseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found with id: " + caseId));

        fraudCase.setStatus(CaseStatus.RESOLVED);
        fraudCase.setFinalDecision(request.getFinalDecision());
        fraudCase.setJustification(request.getJustification());
        fraudCase.setClosedAt(Instant.now());

        // 🔥 fermer alerte
        if (fraudCase.getAlert() != null) {
            fraudCase.getAlert().setStatus(AlertStatus.CLOSED);
            alertRepository.save(fraudCase.getAlert());
        }

        FraudCase savedCase = fraudCaseRepository.save(fraudCase);

        auditLogService.record(
                AuditAction.RESOLVE_CASE,
                "CASE",
                savedCase.getId(),
                savedCase.getOwner(),
                null,
                "Fraud case resolved with decision: " + request.getFinalDecision()
        );

        return toResponse(savedCase);
    }

    // ✅ MAPPING RESPONSE
    private FraudCaseResponse toResponse(FraudCase fraudCase) {

        String transactionRef = fraudCase.getAlert() != null
                && fraudCase.getAlert().getTransaction() != null
                ? fraudCase.getAlert().getTransaction().getTransactionRef()
                : null;

        Long transactionId = fraudCase.getAlert() != null
                && fraudCase.getAlert().getTransaction() != null
                ? fraudCase.getAlert().getTransaction().getIdTransaction()
                : null;

        return FraudCaseResponse.builder()
                .id(fraudCase.getId())
                .caseReference("CASE-" + fraudCase.getId())
                .alertId(fraudCase.getAlert() != null ? fraudCase.getAlert().getId() : null)
                .alertStatus(fraudCase.getAlert() != null ? fraudCase.getAlert().getStatus().name() : null)
                .transactionId(transactionId)
                .transactionRef(transactionRef)
                .ownerId(fraudCase.getOwner() != null ? fraudCase.getOwner().getIdUser() : null)
                .ownerUsername(fraudCase.getOwner() != null ? fraudCase.getOwner().getUsername() : null)
                .status(fraudCase.getStatus())
                .finalDecision(fraudCase.getFinalDecision())
                .title("Fraud Investigation Case")
                .description("Case opened by fraud analyst for investigating a suspicious transaction linked to an alert.")
                .justification(fraudCase.getJustification())
                .openedAt(fraudCase.getOpenedAt())
                .closedAt(fraudCase.getClosedAt())
                .build();
    }
}