package com.example.backendpfe.account;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Page<Account> findAllByIsDeletedFalse(Pageable pageable);

    Optional<Account> findByIdAccountAndIsDeletedFalse(Long idAccount);

    boolean existsByAccountNumberAndIsDeletedFalse(String accountNumber);

    // ✅ filters
    Page<Account> findAllByClient_IdUserAndIsDeletedFalse(Long clientId, Pageable pageable);
    Page<Account> findAllByAnalyst_IdUserAndIsDeletedFalse(Long analystId, Pageable pageable);
    Page<Account> findAllByAuditor_IdUserAndIsDeletedFalse(Long auditorId, Pageable pageable);
}