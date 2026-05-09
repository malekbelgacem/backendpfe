package com.example.backendpfe.transaction;

import com.example.backendpfe.account.Account;
import com.example.backendpfe.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    Page<Transaction> findBySourceAccountOrDestinationAccount(Account sourceAccount, Account destinationAccount, Pageable pageable);

    Page<Transaction> findByInitiatedBy(User user, Pageable pageable);

    Page<Transaction> findBySourceAccountInOrDestinationAccountIn(
            List<Account> sourceAccounts,
            List<Account> destinationAccounts,
            Pageable pageable
    );

    int countBySourceAccountAndTimestampAfter(Account sourceAccount, Instant after);

    List<Transaction> findBySourceAccountAndTimestampAfter(Account sourceAccount, Instant after);

    List<Transaction> findBySourceAccount(Account sourceAccount);
    Optional<Transaction> findByTransfer_Id(Long transferId);
}