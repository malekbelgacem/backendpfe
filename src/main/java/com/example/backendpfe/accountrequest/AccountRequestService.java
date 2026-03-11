package com.example.backendpfe.accountrequest;

import com.example.backendpfe.account.Account;
import com.example.backendpfe.account.AccountRepository;
import com.example.backendpfe.account.AccountStatus;
import com.example.backendpfe.account.dto.AccountResponse;
import com.example.backendpfe.accountrequest.dto.*;
import com.example.backendpfe.user.RoleName;
import com.example.backendpfe.user.User;
import com.example.backendpfe.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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

    private static final String UPLOAD_DIR = "uploads/account-requests/";

    private User getUserOrThrow(Long id, String msg) {
        return userRepository.findByIdUserAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException(msg));
    }

    private void assertRole(User u, RoleName expected) {
        if (u.getRole() == null || u.getRole().getRoleName() != expected) {
            throw new RuntimeException("User " + u.getUsername() + " must have role " + expected);
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
                .clientId(a.getClient().getIdUser())
                .clientUsername(a.getClient().getUsername())
                .analystId(a.getAnalyst().getIdUser())
                .analystUsername(a.getAnalyst().getUsername())
                .auditorId(a.getAuditor().getIdUser())
                .auditorUsername(a.getAuditor().getUsername())
                .build();
    }

    private String saveDocument(MultipartFile document) {
        if (document == null || document.isEmpty()) {
            throw new RuntimeException("Document is required");
        }

        String contentType = document.getContentType();
        if (contentType == null ||
                !(contentType.equals("application/pdf")
                        || contentType.startsWith("image/"))) {
            throw new RuntimeException("Only PDF or image files are allowed");
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
            throw new RuntimeException("Failed to save document");
        }
    }

    // ✅ Client: create enriched request
    public AccountRequestResponse createRequest(
            Long clientId,
            String accountType,
            String description,
            MultipartFile document
    ) {
        User client = getUserOrThrow(clientId, "Client not found");
        assertRole(client, RoleName.CLIENT);

        if (accountType == null || accountType.isBlank()) {
            throw new RuntimeException("Account type is required");
        }

        String normalizedType = accountType.trim().toUpperCase();
        if (!normalizedType.equals("DEBIT") && !normalizedType.equals("CREDIT")) {
            throw new RuntimeException("Account type must be DEBIT or CREDIT");
        }

        if (description == null || description.isBlank()) {
            throw new RuntimeException("Description is required");
        }

        String documentPath = saveDocument(document);

        AccountRequest r = AccountRequest.builder()
                .client(client)
                .status(AccountRequestStatus.PENDING)
                .accountType(normalizedType)
                .description(description)
                .documentPath(documentPath)
                .build();

        requestRepository.save(r);
        return toResponse(r);
    }

    public Page<AccountRequestResponse> myRequests(Long clientId, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("idRequest").descending());
        return requestRepository.findAllByClient_IdUser(clientId, pageable).map(this::toResponse);
    }

    public Page<AccountRequestResponse> listByStatus(String status, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("idRequest").descending());
        AccountRequestStatus st = AccountRequestStatus.valueOf(status.toUpperCase());
        return requestRepository.findAllByStatus(st, pageable).map(this::toResponse);
    }

    public AccountResponse validate(AccountValidateRequest req) {
        AccountRequest r = requestRepository.findById(req.getRequestId())
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (r.getStatus() != AccountRequestStatus.PENDING) {
            throw new RuntimeException("Request already decided");
        }

        if (Boolean.FALSE.equals(req.getApproved())) {
            r.setStatus(AccountRequestStatus.REJECTED);
            r.setDecisionNote(req.getDecisionNote());
            r.setDecidedAt(Instant.now());
            requestRepository.save(r);
            throw new RuntimeException("Request rejected");
        }

        if (req.getAccountNumber() == null || req.getAccountNumber().isBlank()) {
            throw new RuntimeException("Account number is required when approving");
        }
        if (req.getAnalystId() == null) {
            throw new RuntimeException("Analyst is required when approving");
        }
        if (req.getAuditorId() == null) {
            throw new RuntimeException("Auditor is required when approving");
        }

        if (accountRepository.existsByAccountNumberAndIsDeletedFalse(req.getAccountNumber())) {
            throw new RuntimeException("Account number already used");
        }

        User analyst = getUserOrThrow(req.getAnalystId(), "Analyst not found");
        User auditor = getUserOrThrow(req.getAuditorId(), "Auditor not found");

        assertRole(analyst, RoleName.ANALYST);
        assertRole(auditor, RoleName.AUDITOR);

        Account a = Account.builder()
                .accountNumber(req.getAccountNumber())
                .client(r.getClient())
                .analyst(analyst)
                .auditor(auditor)
                .status(AccountStatus.ACTIVE)
                .isDeleted(false)
                .build();

        accountRepository.save(a);

        r.setStatus(AccountRequestStatus.APPROVED);
        r.setDecisionNote(req.getDecisionNote());
        r.setDecidedAt(Instant.now());
        requestRepository.save(r);

        return toAccountResponse(a);
    }
}