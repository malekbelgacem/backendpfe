package com.example.backendpfe.casemanagement;

import com.example.backendpfe.casemanagement.dto.CreateCaseRequest;
import com.example.backendpfe.casemanagement.dto.FraudCaseResponse;
import com.example.backendpfe.casemanagement.dto.ResolveCaseRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cases")
@RequiredArgsConstructor
public class FraudCaseController {

    private final FraudCaseService fraudCaseService;

    @PreAuthorize("hasAnyRole('ANALYST','SUPER_ADMIN')")
    @PostMapping("/from-alert/{alertId}")
    public FraudCaseResponse createFromAlert(
            @PathVariable Long alertId,
            @Valid @RequestBody CreateCaseRequest request,
            Authentication authentication
    ) {
        return fraudCaseService.createFromAlert(alertId, request, authentication.getName());
    }

    @PreAuthorize("hasAnyRole('ANALYST','AUDITOR','SUPER_ADMIN')")
    @GetMapping
    public Page<FraudCaseResponse> getAllCases(Pageable pageable) {
        return fraudCaseService.getAll(pageable);
    }

    @PreAuthorize("hasRole('ANALYST')")
    @GetMapping("/my")
    public Page<FraudCaseResponse> getMyCases(
            Authentication authentication,
            Pageable pageable
    ) {
        return fraudCaseService.getMyCases(authentication.getName(), pageable);
    }

    @PreAuthorize("hasAnyRole('ANALYST','AUDITOR','SUPER_ADMIN')")
    @GetMapping("/status/{status}")
    public Page<FraudCaseResponse> getCasesByStatus(
            @PathVariable CaseStatus status,
            Pageable pageable
    ) {
        return fraudCaseService.getByStatus(status, pageable);
    }

    @PreAuthorize("hasAnyRole('ANALYST','AUDITOR','SUPER_ADMIN')")
    @GetMapping("/{id}")
    public FraudCaseResponse getCaseById(@PathVariable Long id) {
        return fraudCaseService.getById(id);
    }

    @PreAuthorize("hasAnyRole('ANALYST','SUPER_ADMIN')")
    @PutMapping("/{id}/resolve")
    public FraudCaseResponse resolveCase(
            @PathVariable Long id,
            @Valid @RequestBody ResolveCaseRequest request
    ) {
        return fraudCaseService.resolveCase(id, request);
    }
}