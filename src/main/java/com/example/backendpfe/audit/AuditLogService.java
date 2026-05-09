package com.example.backendpfe.audit;

import com.example.backendpfe.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public void record(
            AuditAction action,
            String entity,
            Long entityId,
            User user,
            String ipAddress,
            String description
    ) {
        AuditLog log = AuditLog.builder()
                .action(action)
                .entity(entity)
                .entityId(entityId)
                .user(user)
                .ipAddress(ipAddress)
                .description(description)
                .createdAt(Instant.now())
                .build();

        auditLogRepository.save(log);
    }
}