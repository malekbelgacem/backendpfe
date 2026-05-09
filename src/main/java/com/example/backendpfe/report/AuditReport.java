package com.example.backendpfe.report;

import com.example.backendpfe.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "audit_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String reportReference;

    private String fileName;

    private Instant startDate;
    private Instant endDate;
    private Instant generatedAt;
    private Long caseId;
    private String caseReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_by_id")
    private User generatedBy;

    private String generatedByUsername;

    @Lob
    @Column(nullable = false)
    private byte[] pdfContent;
}