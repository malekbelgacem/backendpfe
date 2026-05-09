package com.example.backendpfe.report.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@NoArgsConstructor
public class AuditReportResponse {

    private Long id;
    private String reportReference;
    private String fileName;
    private Instant startDate;
    private Instant endDate;
    private Instant generatedAt;
    private String generatedByUsername;
    private Long caseId;
    private String caseReference;

    public AuditReportResponse(
            Long id,
            String reportReference,
            String fileName,
            Instant startDate,
            Instant endDate,
            Instant generatedAt,
            String generatedByUsername
    ) {
        this.id = id;
        this.reportReference = reportReference;
        this.fileName = fileName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.generatedAt = generatedAt;
        this.generatedByUsername = generatedByUsername;
    }

    @Builder
    public AuditReportResponse(
            Long id,
            String reportReference,
            String fileName,
            Instant startDate,
            Instant endDate,
            Instant generatedAt,
            String generatedByUsername,
            Long caseId,
            String caseReference
    ) {
        this.id = id;
        this.reportReference = reportReference;
        this.fileName = fileName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.generatedAt = generatedAt;
        this.generatedByUsername = generatedByUsername;
        this.caseId = caseId;
        this.caseReference = caseReference;
    }
}