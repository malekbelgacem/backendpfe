package com.example.backendpfe.report;

import com.example.backendpfe.report.dto.AuditReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PreAuthorize("hasAnyRole('AUDITOR','SUPER_ADMIN')")
    @PostMapping("/audit")
    public AuditReportResponse generateAndSaveAuditReport(
            @RequestParam String startDate,
            @RequestParam String endDate
    ) {
        AuditReport report = reportService.saveAuditReport(
                Instant.parse(startDate),
                Instant.parse(endDate)
        );

        return AuditReportResponse.builder()
                .id(report.getId())
                .reportReference(report.getReportReference())
                .fileName(report.getFileName())
                .startDate(report.getStartDate())
                .endDate(report.getEndDate())
                .generatedAt(report.getGeneratedAt())
                .generatedByUsername(report.getGeneratedByUsername())
                .build();
    }

    @PreAuthorize("hasAnyRole('AUDITOR','SUPER_ADMIN')")
    @GetMapping
    public Page<AuditReportResponse> getReports(Pageable pageable) {
        return reportService.getReports(pageable);
    }

    @PreAuthorize("hasAnyRole('AUDITOR','SUPER_ADMIN')")
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadReport(@PathVariable Long id) {
        byte[] pdf = reportService.downloadReport(id);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=audit-report-" + id + ".pdf")
                .body(pdf);
    }
    @PreAuthorize("hasAnyRole('AUDITOR','SUPER_ADMIN')")
    @PostMapping("/case/{caseId}")
    public AuditReportResponse generateCaseReport(@PathVariable Long caseId) {
        AuditReport report = reportService.saveCaseReport(caseId);

        return AuditReportResponse.builder()
                .id(report.getId())
                .reportReference(report.getReportReference())
                .fileName(report.getFileName())
                .startDate(report.getStartDate())
                .endDate(report.getEndDate())
                .generatedAt(report.getGeneratedAt())
                .generatedByUsername(report.getGeneratedByUsername())
                .caseReference(report.getCaseReference())
                .build();
    }
}