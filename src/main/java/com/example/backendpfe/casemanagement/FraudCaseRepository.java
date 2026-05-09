package com.example.backendpfe.casemanagement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FraudCaseRepository extends JpaRepository<FraudCase, Long> {

    boolean existsByAlert_Id(Long alertId);

    Page<FraudCase> findAllByOrderByOpenedAtDesc(Pageable pageable);

    Page<FraudCase> findByOwner_UsernameOrderByOpenedAtDesc(String username, Pageable pageable);
    Page<FraudCase> findByStatusOrderByOpenedAtDesc(CaseStatus status, Pageable pageable);
}