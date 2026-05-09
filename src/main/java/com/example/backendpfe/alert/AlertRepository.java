package com.example.backendpfe.alert;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    Page<Alert> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Alert> findByStatusOrderByCreatedAtDesc(AlertStatus status, Pageable pageable);

    boolean existsByTransaction_IdTransaction(Long transactionId);
    Page<Alert> findByAssignedTo_UsernameOrderByCreatedAtDesc(
            String username,
            Pageable pageable
    );
}