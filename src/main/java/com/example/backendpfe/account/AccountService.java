package com.example.backendpfe.account;

import com.example.backendpfe.account.dto.*;
import com.example.backendpfe.notification.NotificationService;
import com.example.backendpfe.notification.NotificationType;
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
    private final NotificationService notificationService;

    private AccountResponse toResponse(Account a) {
        return AccountResponse.builder()
                .idAccount(a.getIdAccount())
                .accountNumber(a.getAccountNumber())
                .balance(a.getBalance())
                .status(a.getStatus().name())
                .createdAt(a.getCreatedAt())
                .clientId(a.getClient().getIdUser())
                .clientUsername(a.getClient().getUsername())
                .analystId(a.getAnalyst() != null ? a.getAnalyst().getIdUser() : null)
                .analystUsername(a.getAnalyst() != null ? a.getAnalyst().getUsername() : null)
                .auditorId(a.getAuditor() != null ? a.getAuditor().getIdUser() : null)
                .auditorUsername(a.getAuditor() != null ? a.getAuditor().getUsername() : null)
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

    private void safeNotify(User user, NotificationType type, String title, String body) {
        try {
            if (user == null) return;
            notificationService.createNotification(user, type, title, body);
        } catch (Exception ignored) {
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
                .status(AccountStatus.ACTIVE)
                .isDeleted(false)
                .build();

        accountRepository.save(a);

        safeNotify(
                client,
                NotificationType.ACCOUNT_CREATED,
                "Compte bancaire créé",
                "Votre compte bancaire " + a.getAccountNumber() + " a été créé avec succès."
        );

        return toResponse(a);
    }

    public AccountResponse update(Long id, AccountUpdateRequest req) {
        Account a = accountRepository.findByIdAccountAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        boolean notifyClient = false;

        if (req.getAccountNumber() != null && !req.getAccountNumber().isBlank()) {
            if (!req.getAccountNumber().equals(a.getAccountNumber())
                    && accountRepository.existsByAccountNumberAndIsDeletedFalse(req.getAccountNumber())) {
                throw new RuntimeException("Account number already used");
            }
            a.setAccountNumber(req.getAccountNumber());
            notifyClient = true;
        }

        if (req.getBalance() != null) {
            a.setBalance(req.getBalance());
        }

        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            a.setStatus(AccountStatus.valueOf(req.getStatus().toUpperCase()));
            notifyClient = true;
        }

        if (req.getClientId() != null) {
            User client = getUserOrThrow(req.getClientId(), "Client user not found");
            assertRole(client, RoleName.CLIENT);
            a.setClient(client);
            notifyClient = true;
        }

        if (req.getAnalystId() != null) {
            User analyst = getUserOrThrow(req.getAnalystId(), "Analyst user not found");
            assertRole(analyst, RoleName.ANALYST);
            a.setAnalyst(analyst);
            notifyClient = true;
        }

        if (req.getAuditorId() != null) {
            User auditor = getUserOrThrow(req.getAuditorId(), "Auditor user not found");
            assertRole(auditor, RoleName.AUDITOR);
            a.setAuditor(auditor);
            notifyClient = true;
        }

        accountRepository.save(a);

        if (notifyClient && a.getClient() != null) {
            safeNotify(
                    a.getClient(),
                    NotificationType.ACCOUNT_INFO,
                    "Mise à jour de votre compte",
                    "Votre compte " + a.getAccountNumber()
                            + " a été mis à jour par l’administration."
            );
        }

        return toResponse(a);
    }

    public void delete(Long id) {
        Account a = accountRepository.findByIdAccountAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        a.setIsDeleted(true);
        accountRepository.save(a);

        if (a.getClient() != null) {
            safeNotify(
                    a.getClient(),
                    NotificationType.ACCOUNT_INFO,
                    "Compte désactivé",
                    "Votre compte " + a.getAccountNumber() + " a été désactivé."
            );
        }
    }

    public AccountResponse restore(Long id) {
        Account a = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        a.setIsDeleted(false);
        accountRepository.save(a);

        if (a.getClient() != null) {
            safeNotify(
                    a.getClient(),
                    NotificationType.ACCOUNT_INFO,
                    "Compte restauré",
                    "Votre compte " + a.getAccountNumber() + " a été restauré."
            );
        }

        return toResponse(a);
    }
}