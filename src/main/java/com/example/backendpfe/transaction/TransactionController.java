package com.example.backendpfe.transaction;

import com.example.backendpfe.transaction.dto.CreateTransactionRequest;
import com.example.backendpfe.transaction.dto.TransactionResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    // Création manuelle backoffice
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ANALYST')")
    @PostMapping
    public TransactionResponse createTransaction(@Valid @RequestBody CreateTransactionRequest request) {
        return transactionService.createTransaction(request);
    }

    // Détail transaction
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ANALYST')")
    @GetMapping("/{id}")
    public TransactionResponse getTransactionById(@PathVariable Long id) {
        return transactionService.getTransactionById(id);
    }

    // Toutes les transactions - admin uniquement
    // Toutes les transactions - super admin + analyst
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ANALYST')")
    @GetMapping
    public Page<TransactionResponse> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return transactionService.getAllTransactions(page, size);
    }

    // Recherche / filtre
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ANALYST')")
    @GetMapping("/search")
    public Page<TransactionResponse> searchTransactions(
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) TransactionChannel channel,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "timestamp") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        return transactionService.searchTransactions(
                accountId, status, channel, page, size, sortBy, sortDir
        );
    }

    // Historique client
    @PreAuthorize("hasRole('CLIENT')")
    @GetMapping("/my")
    public Page<TransactionResponse> getMyTransactionHistory(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return transactionService.getClientTransactionHistory(authentication.getName(), page, size);
    }

    // Endpoint principal par rôle
    @PreAuthorize("hasAnyRole('CLIENT','ANALYST','AUDITOR','SUPER_ADMIN')")
    @GetMapping("/me")
    public Page<TransactionResponse> getMyTransactions(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return transactionService.getTransactionsForConnectedUser(
                authentication.getName(), page, size
        );
    }

    // Dernière transaction de l'utilisateur connecté
    @PreAuthorize("hasAnyRole('CLIENT','ANALYST','AUDITOR','SUPER_ADMIN')")
    @GetMapping("/me/latest")
    public TransactionResponse getLatestTransaction(Authentication authentication) {
        Page<TransactionResponse> page = transactionService.getTransactionsForConnectedUser(
                authentication.getName(), 0, 1
        );

        if (page.isEmpty()) {
            throw new EntityNotFoundException("No transactions found for this user");
        }

        return page.getContent().get(0);
    }

    // Test auth temporaire
    @PreAuthorize("hasAnyRole('CLIENT','ANALYST','AUDITOR','SUPER_ADMIN')")
    @GetMapping("/me-test")
    public String testAuth(Authentication authentication) {
        return authentication.getAuthorities().toString();
    }
}