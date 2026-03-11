package com.example.backendpfe.account;

import com.example.backendpfe.account.dto.*;
import com.example.backendpfe.user.RoleName;
import com.example.backendpfe.user.User;
import com.example.backendpfe.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    private AccountResponse toResponse(Account a) {
        return AccountResponse.builder()
                .idAccount(a.getIdAccount())
                .accountNumber(a.getAccountNumber())
                .balance(a.getBalance())
                .status(a.getStatus().name())
                .createdAt(a.getCreatedAt())

                .clientId(a.getClient().getIdUser())
                .clientUsername(a.getClient().getUsername())

                .analystId(a.getAnalyst().getIdUser())
                .analystUsername(a.getAnalyst().getUsername())

                .auditorId(a.getAuditor().getIdUser())
                .auditorUsername(a.getAuditor().getUsername())
                .build();
    }

    private User getUserOrThrow(Long id, String msg) {
        return userRepository.findByIdUserAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException(msg));
    }

    private void assertRole(User u, RoleName expected) {
        if (u.getRole() == null || u.getRole().getRoleName() != expected) {
            throw new RuntimeException("User " + u.getUsername() + " must have role " + expected);
        }
    }

    public Page<AccountResponse> getAll(int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("idAccount").descending());
        return accountRepository.findAllByIsDeletedFalse(pageable).map(this::toResponse);
    }

    public Page<AccountResponse> getByClient(Long clientId, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("idAccount").descending());
        return accountRepository.findAllByClient_IdUserAndIsDeletedFalse(clientId, pageable)
                .map(this::toResponse);
    }

    public Page<AccountResponse> getByAnalyst(Long analystId, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("idAccount").descending());
        return accountRepository.findAllByAnalyst_IdUserAndIsDeletedFalse(analystId, pageable)
                .map(this::toResponse);
    }

    public Page<AccountResponse> getByAuditor(Long auditorId, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("idAccount").descending());
        return accountRepository.findAllByAuditor_IdUserAndIsDeletedFalse(auditorId, pageable)
                .map(this::toResponse);
    }

    public AccountResponse getById(Long id) {
        Account a = accountRepository.findByIdAccountAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        return toResponse(a);
    }

    public AccountResponse create(AccountCreateRequest req) {

        if (accountRepository.existsByAccountNumberAndIsDeletedFalse(req.getAccountNumber())) {
            throw new RuntimeException("Account number already used");
        }

        User client = getUserOrThrow(req.getClientId(), "Client user not found");
        User analyst = getUserOrThrow(req.getAnalystId(), "Analyst user not found");
        User auditor = getUserOrThrow(req.getAuditorId(), "Auditor user not found");

        assertRole(client, RoleName.CLIENT);
        assertRole(analyst, RoleName.ANALYST);
        assertRole(auditor, RoleName.AUDITOR);

        Account a = Account.builder()
                .accountNumber(req.getAccountNumber())
                .client(client)
                .analyst(analyst)
                .auditor(auditor)
                .status(AccountStatus.ACTIVE) // default
                .isDeleted(false)
                .build();

        accountRepository.save(a);
        return toResponse(a);
    }

    public AccountResponse update(Long id, AccountUpdateRequest req) {
        Account a = accountRepository.findByIdAccountAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (req.getAccountNumber() != null && !req.getAccountNumber().isBlank()) {
            if (!req.getAccountNumber().equals(a.getAccountNumber())
                    && accountRepository.existsByAccountNumberAndIsDeletedFalse(req.getAccountNumber())) {
                throw new RuntimeException("Account number already used");
            }
            a.setAccountNumber(req.getAccountNumber());
        }

        if (req.getBalance() != null) {
            a.setBalance(req.getBalance());
        }

        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            a.setStatus(AccountStatus.valueOf(req.getStatus().toUpperCase()));
        }

        if (req.getClientId() != null) {
            User client = getUserOrThrow(req.getClientId(), "Client user not found");
            assertRole(client, RoleName.CLIENT);
            a.setClient(client);
        }

        if (req.getAnalystId() != null) {
            User analyst = getUserOrThrow(req.getAnalystId(), "Analyst user not found");
            assertRole(analyst, RoleName.ANALYST);
            a.setAnalyst(analyst);
        }

        if (req.getAuditorId() != null) {
            User auditor = getUserOrThrow(req.getAuditorId(), "Auditor user not found");
            assertRole(auditor, RoleName.AUDITOR);
            a.setAuditor(auditor);
        }

        accountRepository.save(a);
        return toResponse(a);
    }

    // ✅ Soft delete
    public void delete(Long id) {
        Account a = accountRepository.findByIdAccountAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        a.setIsDeleted(true);
        accountRepository.save(a);
    }

    // ✅ Restore
    public AccountResponse restore(Long id) {
        Account a = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        a.setIsDeleted(false);
        accountRepository.save(a);
        return toResponse(a);
    }
}