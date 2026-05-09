package com.example.backendpfe.audit.dto;

import com.example.backendpfe.audit.AuditAction;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class AuditLogResponse {

    private Long id;
    private AuditAction action;
    private String entity;
    private Long entityId;
    private String description;
    private String ipAddress;
    private Instant createdAt;

    private Long userId;
    private String username;
    private String role;
}