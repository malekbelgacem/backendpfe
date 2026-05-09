package com.example.backendpfe.alert;

import com.example.backendpfe.alert.dto.AlertResponse;
import com.example.backendpfe.alert.dto.UpdateAlertStatusRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @PreAuthorize("hasAnyRole('ANALYST', 'AUDITOR', 'SUPER_ADMIN')")
    @GetMapping
    public Page<AlertResponse> getAlerts(
            @RequestParam(required = false) AlertStatus status,
            Pageable pageable
    ) {
        if (status != null) {
            return alertService.getByStatus(status, pageable);
        }
        return alertService.getAll(pageable);
    }
    @PreAuthorize("hasRole('ANALYST')")
    @GetMapping("/my")
    public Page<AlertResponse> getMyAlerts(
            Authentication authentication,
            Pageable pageable
    ) {
        return alertService.getMyAlerts(authentication.getName(), pageable);
    }

    @PreAuthorize("hasAnyRole('ANALYST', 'AUDITOR', 'SUPER_ADMIN')")
    @GetMapping("/{id}")
    public AlertResponse getAlertById(@PathVariable Long id) {
        return alertService.getById(id);
    }
    @PreAuthorize("hasAnyRole('ANALYST','SUPER_ADMIN')")
    @PutMapping("/{id}/status")
    public AlertResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAlertStatusRequest request
    ) {
        return alertService.updateStatus(id, request);
    }

}