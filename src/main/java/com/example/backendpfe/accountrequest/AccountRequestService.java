package com.example.backendpfe.accountrequest;

import com.example.backendpfe.account.Account;
import com.example.backendpfe.account.AccountRepository;
import com.example.backendpfe.account.AccountStatus;
import com.example.backendpfe.account.dto.AccountResponse;
import com.example.backendpfe.accountrequest.dto.AccountRequestResponse;
import com.example.backendpfe.accountrequest.dto.AccountValidateRequest;
import com.example.backendpfe.exception.BadRequestException;
import com.example.backendpfe.exception.ResourceNotFoundException;
import com.example.backendpfe.notification.NotificationService;
import com.example.backendpfe.notification.NotificationType;
import com.example.backendpfe.user.RoleName;
import com.example.backendpfe.user.User;
import com.example.backendpfe.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.example.backendpfe.accountrequest.dto.AccountRequestLatestStatusResponse;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountRequestService {

    private final AccountRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final NotificationService notificationService;

    private static final String UPLOAD_DIR = "uploads/account-requests/";

    private User getUserOrThrow(Long id, String msg) {
        return userRepository.findByIdUserAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(msg));
    }

    private void assertRole(User u, RoleName expected) {
        if (u.getRole() == null || u.getRole().getRoleName() != expected) {
            throw new BadRequestException("User " + u.getUsername() + " must have role " + expected);
        }
    }

    private AccountRequestResponse toResponse(AccountRequest r) {
        return AccountRequestResponse.builder()
                .idRequest(r.getIdRequest())
                .status(r.getStatus().name())
                .createdAt(r.getCreatedAt())
                .decidedAt(r.getDecidedAt())
                .clientId(r.getClient().getIdUser())
                .clientUsername(r.getClient().getUsername())
                .decisionNote(r.getDecisionNote())
                .accountType(r.getAccountType())
                .description(r.getDescription())
                .documentPath(r.getDocumentPath())
                .build();
    }

    private AccountResponse toAccountResponse(Account a) {
        return AccountResponse.builder()
                .idAccount(a.getIdAccount())
                .accountNumber(a.getAccountNumber())
                .balance(a.getBalance())
                .status(a.getStatus().name())
                .createdAt(a.getCreatedAt())
                .clientId(a.getClient() != null ? a.getClient().getIdUser() : null)
                .clientUsername(a.getClient() != null ? a.getClient().getUsername() : null)
                .analystId(a.getAnalyst() != null ? a.getAnalyst().getIdUser() : null)
                .analystUsername(a.getAnalyst() != null ? a.getAnalyst().getUsername() : null)
                .auditorId(a.getAuditor() != null ? a.getAuditor().getIdUser() : null)
                .auditorUsername(a.getAuditor() != null ? a.getAuditor().getUsername() : null)
                .build();
    }

    private String saveDocument(MultipartFile document) {
        if (document == null || document.isEmpty()) {
            throw new BadRequestException("Document is required");
        }

        String contentType = document.getContentType();
        if (contentType == null ||
                !(contentType.equals("application/pdf") || contentType.startsWith("image/"))) {
            throw new BadRequestException("Only PDF or image files are allowed");
        }

        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            Files.createDirectories(uploadPath);

            String originalName = document.getOriginalFilename();
            String fileName = UUID.randomUUID() + "_" + originalName;

            Path filePath = uploadPath.resolve(fileName);
            Files.copy(document.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return filePath.toString();
        } catch (IOException e) {
            throw new BadRequestException("Failed to save document");
        }
    }

    private void safeNotify(User user, NotificationType type, String title, String body) {
        try {
            if (user == null) {
                System.out.println("safeNotify -> user is null");
                return;
            }

            notificationService.createNotification(user, type, title, body);

            System.out.println(
                    "Notification created successfully -> user: " + user.getUsername()
                            + " | type: " + type
                            + " | title: " + title
            );

        } catch (Exception e) {
            System.out.println("Notification error -> " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Transactional
    public AccountRequestResponse createRequest(
            Long clientId,
            String accountType,
            String description,
            MultipartFile document
    ) {
        User client = getUserOrThrow(clientId, "Client not found");
        assertRole(client, RoleName.CLIENT);

        if (accountType == null || accountType.isBlank()) {
            throw new BadRequestException("Account type is required");
        }

        String normalizedType = accountType.trim().toUpperCase();
        if (!normalizedType.equals("DEBIT") && !normalizedType.equals("CREDIT")) {
            throw new BadRequestException("Account type must be DEBIT or CREDIT");
        }

        if (description == null || description.isBlank()) {
            throw new BadRequestException("Description is required");
        }

        String documentPath = saveDocument(document);

        AccountRequest request = AccountRequest.builder()
                .client(client)
                .status(AccountRequestStatus.PENDING)
                .accountType(normalizedType)
                .description(description.trim())
                .documentPath(documentPath)
                .build();

        request = requestRepository.save(request);

        safeNotify(
                client,
                NotificationType.ACCOUNT_REQUEST_CREATED,
                "Demande de compte envoyée",
                "Votre demande de création de compte a bien été enregistrée et sera examinée prochainement."
        );

        return toResponse(request);
    }

    public Page<AccountRequestResponse> myRequests(Long clientId, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("idRequest").descending());
        return requestRepository.findAllByClient_IdUser(clientId, pageable).map(this::toResponse);
    }

    public Page<AccountRequestResponse> listByStatus(String status, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("idRequest").descending());

        AccountRequestStatus st;
        try {
            st = AccountRequestStatus.valueOf(status.toUpperCase());
        } catch (Exception e) {
            throw new BadRequestException("Invalid status. Use PENDING, APPROVED or REJECTED");
        }

        return requestRepository.findAllByStatus(st, pageable).map(this::toResponse);
    }

    @Transactional
    public AccountResponse validate(AccountValidateRequest req) {
        AccountRequest request = requestRepository.findById(req.getRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        if (request.getStatus() != AccountRequestStatus.PENDING) {
            throw new BadRequestException("Request already decided");
        }

        User client = request.getClient();
        request.setDecisionNote(req.getDecisionNote());
        request.setDecidedAt(Instant.now());

        // REJECT
        if (Boolean.FALSE.equals(req.getApproved())) {
            request.setStatus(AccountRequestStatus.REJECTED);
            requestRepository.save(request);

            safeNotify(
                    client,
                    NotificationType.ACCOUNT_REQUEST_REJECTED,
                    "Demande de compte rejetée",
                    "Votre demande de création de compte a été rejetée."
                            + (req.getDecisionNote() != null && !req.getDecisionNote().isBlank()
                            ? " Motif : " + req.getDecisionNote()
                            : "")
            );

            throw new BadRequestException("Request rejected");
        }

        // APPROVE
        if (req.getAccountNumber() == null || req.getAccountNumber().isBlank()) {
            throw new BadRequestException("Account number is required when approving");
        }
        if (req.getAnalystId() == null) {
            throw new BadRequestException("Analyst is required when approving");
        }
        if (req.getAuditorId() == null) {
            throw new BadRequestException("Auditor is required when approving");
        }

        if (accountRepository.existsByAccountNumberAndIsDeletedFalse(req.getAccountNumber().trim())) {
            throw new BadRequestException("Account number already used");
        }

        User analyst = getUserOrThrow(req.getAnalystId(), "Analyst not found");
        User auditor = getUserOrThrow(req.getAuditorId(), "Auditor not found");

        assertRole(analyst, RoleName.ANALYST);
        assertRole(auditor, RoleName.AUDITOR);

        Account account = Account.builder()
                .accountNumber(req.getAccountNumber().trim())
                .client(client)
                .analyst(analyst)
                .auditor(auditor)
                .status(AccountStatus.ACTIVE)
                .isDeleted(false)
                .build();

        account = accountRepository.save(account);

        request.setStatus(AccountRequestStatus.APPROVED);
        requestRepository.save(request);

        System.out.println("APPROVE REQUEST -> client = " + client.getUsername());
        System.out.println("APPROVE REQUEST -> request id = " + request.getIdRequest());
        System.out.println("APPROVE REQUEST -> account number = " + account.getAccountNumber());

        safeNotify(
                client,
                NotificationType.ACCOUNT_REQUEST_APPROVED,
                "Demande de compte approuvée",
                "Votre demande de création de compte a été approuvée."
        );

        safeNotify(
                client,
                NotificationType.ACCOUNT_CREATED,
                "Compte bancaire créé",
                "Votre compte bancaire " + account.getAccountNumber() + " a été créé et affecté avec succès."
        );

        return toAccountResponse(account);
    }
    public AccountRequestLatestStatusResponse getLatestRequestStatus(Long clientId) {
        User client = getUserOrThrow(clientId, "Client not found");
        assertRole(client, RoleName.CLIENT);

        return requestRepository.findTopByClient_IdUserOrderByCreatedAtDesc(clientId)
                .map(req -> AccountRequestLatestStatusResponse.builder()
                        .exists(true)
                        .requestId(req.getIdRequest())
                        .status(req.getStatus().name())
                        .accountType(req.getAccountType())
                        .description(req.getDescription())
                        .build())
                .orElse(AccountRequestLatestStatusResponse.builder()
                        .exists(false)
                        .requestId(null)
                        .status(null)
                        .accountType(null)
                        .description(null)
                        .build());
    }
}