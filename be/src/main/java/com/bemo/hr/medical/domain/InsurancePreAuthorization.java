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
@Table(name = "insurance_pre_authorizations")
@Getter
@Setter
@NoArgsConstructor
public class InsurancePreAuthorization {

    public enum Status {
        REQUESTED, APPROVED, REJECTED, EXPIRED
    }

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "payer_id", length = 36, nullable = false)
    private String payerId;

    @Column(name = "patient_id", length = 36, nullable = false)
    private String patientId;

    @Column(name = "visit_id", length = 36)
    private String visitId;

    @Column(name = "procedure_text", length = 255, nullable = false)
    private String procedureText;

    @Column(name = "approval_code", length = 60, nullable = false)
    private String approvalCode;

    @Column(name = "requested_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal requestedAmount;

    @Column(name = "approved_amount", precision = 15, scale = 2)
    private BigDecimal approvedAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private Status status = Status.REQUESTED;

    @Column(name = "decided_at")
    private Long decidedAt;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public InsurancePreAuthorization(String payerId, String patientId, String visitId,
                                     String procedureText, String approvalCode,
                                     BigDecimal requestedAmount) {
        this.id = UUID.randomUUID().toString();
        this.payerId = payerId;
        this.patientId = patientId;
        this.visitId = visitId;
        this.procedureText = procedureText;
        this.approvalCode = approvalCode;
        this.requestedAmount = requestedAmount;
        this.status = Status.REQUESTED;
        this.createdAt = Instant.now().toEpochMilli();
        this.updatedAt = this.createdAt;
    }

    public void approve(BigDecimal approvedAmount) {
        this.status = Status.APPROVED;
        this.approvedAmount = approvedAmount != null ? approvedAmount : this.requestedAmount;
        this.decidedAt = Instant.now().toEpochMilli();
        this.updatedAt = this.decidedAt;
    }

    public void reject() {
        this.status = Status.REJECTED;
        this.approvedAmount = BigDecimal.ZERO;
        this.decidedAt = Instant.now().toEpochMilli();
        this.updatedAt = this.decidedAt;
    }
}
