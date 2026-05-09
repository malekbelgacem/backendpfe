package com.example.backendpfe.transfer;

import com.example.backendpfe.account.Account;
import com.example.backendpfe.account.AccountRepository;
import com.example.backendpfe.audit.AuditAction;
import com.example.backendpfe.audit.AuditLogService;

import com.example.backendpfe.transaction.Transaction;
import com.example.backendpfe.transaction.TransactionRepository;
import com.example.backendpfe.transaction.TransactionService;
import com.example.backendpfe.transfer.dto.CreateTransferRequest;
import com.example.backendpfe.transfer.dto.TransferResponse;
import com.example.backendpfe.user.User;
import com.example.backendpfe.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import com.example.backendpfe.transaction.TransactionStatus;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final TransferRepository transferRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionService transactionService;
    private final AuditLogService auditLogService;
    private final TransferNotificationService transferNotificationService;
    private final TransactionRepository transactionRepository;

    public TransferResponse createTransfer(CreateTransferRequest request, String username) {
        User currentUser = userRepository.findByUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Account sender = accountRepository.findById(request.getSenderAccountId())
                .orElseThrow(() -> new EntityNotFoundException("Sender account not found"));

        Account receiver = accountRepository.findById(request.getReceiverAccountId())
                .orElseThrow(() -> new EntityNotFoundException("Receiver account not found"));

        if (sender.getClient() == null || !sender.getClient().getIdUser().equals(currentUser.getIdUser())) {
            throw new IllegalArgumentException("You can only transfer from your own account");
        }

        Transfer transfer = Transfer.builder()
                .accountSender(sender)
                .accountReceiver(receiver)
                .amount(request.getAmount())
                .currency(request.getCurrency().toUpperCase())
                .reason(request.getReason())
                .type(request.getType())
                .transactionType(request.getTransactionType())
                .status(TransferStatus.PENDING)
                .build();

        Transfer savedTransfer = transferRepository.save(transfer);

        auditLogService.record(
                AuditAction.CREATE_TRANSFER,
                "TRANSFER",
                savedTransfer.getId(),
                currentUser,
                null,
                "Client created transfer request from account "
                        + sender.getAccountNumber()
                        + " to account "
                        + receiver.getAccountNumber()
                        + " amount "
                        + savedTransfer.getAmount()
                        + " "
                        + savedTransfer.getCurrency()
        );

        autoProcessTransfer(savedTransfer, currentUser);

        Transfer updatedTransfer = transferRepository.findById(savedTransfer.getId())
                .orElse(savedTransfer);

        return mapToResponse(updatedTransfer);
    }

    private void autoProcessTransfer(Transfer transfer, User currentUser) {
        Account sender = transfer.getAccountSender();
        Account receiver = transfer.getAccountReceiver();

        if (sender.getIdAccount().equals(receiver.getIdAccount())) {
            rejectTransfer(transfer, currentUser, "Sender and receiver accounts cannot be the same");
            return;
        }

        if (transfer.getAmount() == null || transfer.getAmount().signum() <= 0) {
            rejectTransfer(transfer, currentUser, "Invalid transfer amount");
            return;
        }

        if (sender.getBalance().compareTo(transfer.getAmount()) < 0) {
            rejectTransfer(transfer, currentUser, "Insufficient balance");
            return;
        }

        // 1) Créer la transaction + appeler IA/scoring
        Transaction tx = transactionService.createFromTransfer(transfer);

        System.out.println("TRANSACTION CREATED FROM TRANSFER = " + tx.getTransactionRef());

        if (tx.getStatus() == TransactionStatus.BLOCKED) {
            transfer.setStatus(TransferStatus.BLOCKED_SECURITY);
            transfer.setValidatedAt(null);
            transfer.setRejectionReason("Transaction bloquée pour vérification de sécurité");

            Transfer blockedTransfer = transferRepository.save(transfer);

            transferNotificationService.createTransferBlockedNotification(
                    sender,
                    blockedTransfer.getAmount(),
                    receiver.getAccountNumber()
            );

            auditLogService.record(
                    AuditAction.REJECT_TRANSFER,
                    "TRANSFER",
                    blockedTransfer.getId(),
                    currentUser,
                    null,
                    "Transfer blocked by security rules. Transaction status: " + tx.getStatus()
            );

            return;
        }

        // 3) Si risque LOW, on valide vraiment le transfert
        sender.setBalance(sender.getBalance().subtract(transfer.getAmount()));
        receiver.setBalance(receiver.getBalance().add(transfer.getAmount()));

        accountRepository.save(sender);
        accountRepository.save(receiver);

        transfer.setStatus(TransferStatus.VALIDATED);
        transfer.setValidatedAt(Instant.now());
        transfer.setRejectionReason(null);

        Transfer validatedTransfer = transferRepository.save(transfer);

        transferNotificationService.createTransferApprovedNotification(
                sender,
                receiver,
                validatedTransfer.getAmount()
        );

        auditLogService.record(
                AuditAction.APPROVE_TRANSFER,
                "TRANSFER",
                validatedTransfer.getId(),
                currentUser,
                null,
                "Transfer automatically validated. Amount "
                        + validatedTransfer.getAmount()
                        + " "
                        + validatedTransfer.getCurrency()
                        + " from account "
                        + sender.getAccountNumber()
                        + " to account "
                        + receiver.getAccountNumber()
        );
    }

    private void rejectTransfer(Transfer transfer, User currentUser, String reason) {
        transfer.setStatus(TransferStatus.REJECTED);
        transfer.setRejectionReason(reason);
        transfer.setValidatedAt(null);

        Transfer rejectedTransfer = transferRepository.save(transfer);

        transferNotificationService.createTransferRejectedNotification(
                transfer.getAccountSender(),
                "Virement rejeté",
                "Votre virement a été rejeté. Raison : " + reason
        );

        auditLogService.record(
                AuditAction.REJECT_TRANSFER,
                "TRANSFER",
                rejectedTransfer.getId(),
                currentUser,
                null,
                "Transfer rejected. Reason: " + reason
        );
    }

    public List<TransferResponse> getMyTransfers(String username) {
        User currentUser = userRepository.findByUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        List<Account> accounts = accountRepository.findByClientAndIsDeletedFalse(currentUser);

        return accounts.stream()
                .flatMap(account -> transferRepository.findByAccountSenderOrAccountReceiver(account, account).stream())
                .distinct()
                .map(this::mapToResponse)
                .toList();
    }

    private TransferResponse mapToResponse(Transfer transfer) {
        return TransferResponse.builder()
                .id(transfer.getId())
                .senderAccountId(transfer.getAccountSender() != null ? transfer.getAccountSender().getIdAccount() : null)
                .senderAccountNumber(transfer.getAccountSender() != null ? transfer.getAccountSender().getAccountNumber() : null)
                .receiverAccountId(transfer.getAccountReceiver() != null ? transfer.getAccountReceiver().getIdAccount() : null)
                .receiverAccountNumber(transfer.getAccountReceiver() != null ? transfer.getAccountReceiver().getAccountNumber() : null)
                .amount(transfer.getAmount())
                .currency(transfer.getCurrency())
                .reason(transfer.getReason())
                .type(transfer.getType())
                .status(transfer.getStatus())
                .transactionType(transfer.getTransactionType())
                .createdAt(transfer.getCreatedAt())
                .validatedAt(transfer.getValidatedAt())
                .rejectionReason(transfer.getRejectionReason())
                .build();
    }
    public TransferResponse approveBlockedTransfer(Long transferId, String analystUsername) {
        User analyst = userRepository.findByUsernameAndIsDeletedFalse(analystUsername)
                .orElseThrow(() -> new EntityNotFoundException("Analyst not found"));

        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new EntityNotFoundException("Transfer not found"));

        if (transfer.getStatus() != TransferStatus.BLOCKED_SECURITY) {
            throw new IllegalStateException("Only blocked transfers can be approved");
        }

        Account sender = transfer.getAccountSender();
        Account receiver = transfer.getAccountReceiver();

        if (sender.getBalance().compareTo(transfer.getAmount()) < 0) {
            rejectTransfer(transfer, analyst, "Insufficient balance during analyst approval");
            return mapToResponse(transfer);
        }

        sender.setBalance(sender.getBalance().subtract(transfer.getAmount()));
        receiver.setBalance(receiver.getBalance().add(transfer.getAmount()));

        accountRepository.save(sender);
        accountRepository.save(receiver);

        transfer.setStatus(TransferStatus.VALIDATED);
        transfer.setValidatedAt(Instant.now());
        transfer.setRejectionReason(null);

        Transfer savedTransfer = transferRepository.save(transfer);

        Transaction tx = transactionRepository.findByTransfer_Id(transferId)
                .orElseThrow(() -> new EntityNotFoundException("Transaction linked to transfer not found"));

        tx.setStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(tx);

        transferNotificationService.createTransferApprovedNotification(
                sender,
                receiver,
                savedTransfer.getAmount()
        );

        auditLogService.record(
                AuditAction.APPROVE_TRANSFER,
                "TRANSFER",
                savedTransfer.getId(),
                analyst,
                null,
                "Blocked transfer approved by analyst"
        );

        return mapToResponse(savedTransfer);
    }
}