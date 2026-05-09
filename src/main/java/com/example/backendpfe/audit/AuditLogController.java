package com.example.backendpfe.audit;

import com.example.backendpfe.audit.dto.AuditLogResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    @PreAuthorize("hasAnyRole('AUDITOR','SUPER_ADMIN')")
    @GetMapping
    public List<AuditLogResponse> getAll() {
        return auditLogRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .action(log.getAction())
                .entity(log.getEntity())
                .entityId(log.getEntityId())
                .description(log.getDescription())
                .ipAddress(log.getIpAddress())
                .createdAt(log.getCreatedAt())
                .userId(log.getUser() != null ? log.getUser().getIdUser() : null)
                .username(log.getUser() != null ? log.getUser().getUsername() : null)
                .role(
                        log.getUser() != null && log.getUser().getRole() != null
                                ? log.getUser().getRole().getRoleName().name()
                                : null
                )
                .build();
    }
}