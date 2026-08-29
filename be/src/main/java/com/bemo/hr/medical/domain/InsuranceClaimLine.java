package com.bemo.hr.medical.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "insurance_claim_lines")
@Getter
@Setter
@NoArgsConstructor
public class InsuranceClaimLine {

    public enum Status {
        PENDING, APPROVED, REJECTED
    }

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "batch_id", length = 36, nullable = false)
    private String batchId;

    @Column(name = "visit_id", length = 36, nullable = false)
    private String visitId;

    @Column(name = "patient_id", length = 36, nullable = false)
    private String patientId;

    @Column(name = "patient_mrn", length = 30, nullable = false)
    private String patientMrn;

    @Column(name = "patient_name", length = 160, nullable = false)
    private String patientName;

    @Column(name = "member_number", length = 60)
    private String memberNumber;

    @Column(name = "procedure_text", length = 255)
    private String procedureText;

    @Column(name = "total_fee", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalFee;

    @Column(name = "insurer_share", precision = 15, scale = 2, nullable = false)
    private BigDecimal insurerShare;

    @Column(name = "patient_share", precision = 15, scale = 2, nullable = false)
    private BigDecimal patientShare;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private Status status = Status.PENDING;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "resubmitted_line_id", length = 36)
    private String resubmittedLineId;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public InsuranceClaimLine(String batchId, String visitId, String patientId,
                              String patientMrn, String patientName, String memberNumber,
                              String procedureText, BigDecimal totalFee,
                              BigDecimal insurerShare, BigDecimal patientShare) {
        this.id = UUID.randomUUID().toString();
        this.batchId = batchId;
        this.visitId = visitId;
        this.patientId = patientId;
        this.patientMrn = patientMrn;
        this.patientName = patientName;
        this.memberNumber = memberNumber;
        this.procedureText = procedureText;
        this.totalFee = totalFee;
        this.insurerShare = insurerShare;
        this.patientShare = patientShare;
        this.status = Status.PENDING;
        this.createdAt = Instant.now().toEpochMilli();
    }

    public void approve() {
        this.status = Status.APPROVED;
        this.rejectionReason = null;
    }

    public void reject(String reason) {
        this.status = Status.REJECTED;
        this.rejectionReason = reason;
    }
}
