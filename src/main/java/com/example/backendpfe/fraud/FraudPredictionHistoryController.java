package com.example.backendpfe.fraud;

import com.example.backendpfe.fraud.dto.FraudPredictionHistoryResponse;
import com.example.backendpfe.transaction.RiskLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fraud/history")
@RequiredArgsConstructor
public class FraudPredictionHistoryController {

    private final FraudPredictionHistoryService service;

    @PreAuthorize("hasAnyRole('ANALYST', 'AUDITOR', 'SUPER_ADMIN')")
    @GetMapping
    public Page<FraudPredictionHistoryResponse> getHistory(
            @RequestParam(required = false) RiskLevel riskLevel,
            @RequestParam(required = false) Long transactionId,
            Pageable pageable
    ) {
        if (riskLevel != null) {
            return service.getByRiskLevel(riskLevel, pageable);
        }

        if (transactionId != null) {
            return service.getByTransactionId(transactionId, pageable);
        }

        return service.getAll(pageable);
    }

    @PreAuthorize("hasAnyRole('ANALYST', 'AUDITOR', 'SUPER_ADMIN')")
    @GetMapping("/{id}")
    public FraudPredictionHistoryResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }
}