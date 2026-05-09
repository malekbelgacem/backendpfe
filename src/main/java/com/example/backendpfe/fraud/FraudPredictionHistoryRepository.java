package com.example.backendpfe.fraud;

import com.example.backendpfe.transaction.RiskLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FraudPredictionHistoryRepository extends JpaRepository<FraudPredictionHistory, Long> {

    Page<FraudPredictionHistory> findAllByOrderByPredictedAtDesc(Pageable pageable);

    Page<FraudPredictionHistory> findByRiskLevelOrderByPredictedAtDesc(RiskLevel riskLevel, Pageable pageable);

    Page<FraudPredictionHistory> findByTransaction_IdTransactionOrderByPredictedAtDesc(Long transactionId, Pageable pageable);
}