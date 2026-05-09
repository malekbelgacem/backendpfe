package com.example.backendpfe.fraud;

import com.example.backendpfe.fraud.dto.FraudPredictionHistoryResponse;
import com.example.backendpfe.transaction.RiskLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FraudPredictionHistoryService {

    private final FraudPredictionHistoryRepository repository;

    public Page<FraudPredictionHistoryResponse> getAll(Pageable pageable) {
        return repository.findAllByOrderByPredictedAtDesc(pageable)
                .map(this::toResponse);
    }

    public Page<FraudPredictionHistoryResponse> getByRiskLevel(RiskLevel riskLevel, Pageable pageable) {
        return repository.findByRiskLevelOrderByPredictedAtDesc(riskLevel, pageable)
                .map(this::toResponse);
    }

    public Page<FraudPredictionHistoryResponse> getByTransactionId(Long transactionId, Pageable pageable) {
        return repository.findByTransaction_IdTransactionOrderByPredictedAtDesc(transactionId, pageable)
                .map(this::toResponse);
    }

    public FraudPredictionHistoryResponse getById(Long id) {
        FraudPredictionHistory history = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Prediction history not found with id: " + id));

        return toResponse(history);
    }

    private FraudPredictionHistoryResponse toResponse(FraudPredictionHistory history) {
        return FraudPredictionHistoryResponse.builder()
                .id(history.getId())
                .transactionId(history.getTransaction().getIdTransaction())
                .transactionRef(history.getTransaction().getTransactionRef())
                .fraudScore(history.getFraudScore())
                .riskLevel(history.getRiskLevel())
                .scoringStatus(history.getScoringStatus())
                .reason(history.getReason())
                .modelVersion(history.getModelVersion())
                .predictedAt(history.getPredictedAt())
                .build();
    }
}
