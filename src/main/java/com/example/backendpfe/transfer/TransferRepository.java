package com.example.backendpfe.transfer;

import com.example.backendpfe.account.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransferRepository extends JpaRepository<Transfer, Long> {
    List<Transfer> findByAccountSenderOrAccountReceiver(Account sender, Account receiver);
}