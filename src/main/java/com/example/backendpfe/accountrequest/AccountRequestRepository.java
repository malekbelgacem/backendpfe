package com.example.backendpfe.accountrequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRequestRepository extends JpaRepository<AccountRequest, Long> {
    Page<AccountRequest> findAllByStatus(AccountRequestStatus status, Pageable pageable);
    Page<AccountRequest> findAllByClient_IdUser(Long clientId, Pageable pageable);
}