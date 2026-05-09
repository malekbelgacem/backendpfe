package com.example.backendpfe.report;

import com.example.backendpfe.report.dto.AuditReportResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AuditReportRepository extends JpaRepository<AuditReport, Long> {

    @Query("""
        select new com.example.backendpfe.report.dto.AuditReportResponse(
            r.id,
            r.reportReference,
            r.fileName,
            r.startDate,
            r.endDate,
            r.generatedAt,
            r.generatedByUsername
        )
        from AuditReport r
        order by r.generatedAt desc
    """)
    Page<AuditReportResponse> findReportsWithoutPdf(Pageable pageable);
}