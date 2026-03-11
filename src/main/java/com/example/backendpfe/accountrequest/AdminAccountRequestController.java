package com.example.backendpfe.accountrequest;

import com.example.backendpfe.account.dto.AccountResponse;
import com.example.backendpfe.accountrequest.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/account-requests")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminAccountRequestController {

    private final AccountRequestService service;

    // ✅ list requests by status: PENDING/APPROVED/REJECTED
    @GetMapping
    public ResponseEntity<Page<AccountRequestResponse>> list(
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(service.listByStatus(status, page, size));
    }

    // ✅ validate: approve/reject + create account if approve
    @PostMapping("/validate")
    public ResponseEntity<AccountResponse> validate(@Valid @RequestBody AccountValidateRequest req) {
        return ResponseEntity.ok(service.validate(req));
    }
}