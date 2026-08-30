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
@Table(name = "insurance_claim_batches")
@Getter
@Setter
@NoArgsConstructor
public class InsuranceClaimBatch {

    public enum Status {
        DRAFT, SUBMITTED, PARTIALLY_PAID, PAID, REJECTED
    }

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "batch_number", length = 50, nullable = false)
    private String batchNumber;

    @Column(name = "payer_id", length = 36, nullable = false)
    private String payerId;

    @Column(name = "period", length = 20, nullable = false)
    private String period;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private Status status = Status.DRAFT;

    @Column(name = "total_claimed_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalClaimedAmount = BigDecimal.ZERO;

    @Column(name = "total_approved_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalApprovedAmount = BigDecimal.ZERO;

    @Column(name = "total_rejected_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalRejectedAmount = BigDecimal.ZERO;

    @Column(name = "submitted_at")
    private Long submittedAt;

    @Column(name = "settled_at")
    private Long settledAt;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public InsuranceClaimBatch(String batchNumber, String payerId, String period, String notes) {
        this.id = UUID.randomUUID().toString();
        this.batchNumber = batchNumber;
        this.payerId = payerId;
        this.period = period;
        this.status = Status.DRAFT;
        this.totalClaimedAmount = BigDecimal.ZERO;
        this.totalApprovedAmount = BigDecimal.ZERO;
        this.totalRejectedAmount = BigDecimal.ZERO;
        this.notes = notes;
        this.createdAt = Instant.now().toEpochMilli();
        this.updatedAt = this.createdAt;
    }

    public void submit() {
        this.status = Status.SUBMITTED;
        this.submittedAt = Instant.now().toEpochMilli();
        this.updatedAt = this.submittedAt;
    }

    public void settle(BigDecimal approvedAmount, BigDecimal rejectedAmount) {
        this.totalApprovedAmount = approvedAmount != null ? approvedAmount : BigDecimal.ZERO;
        this.totalRejectedAmount = rejectedAmount != null ? rejectedAmount : BigDecimal.ZERO;
        this.settledAt = Instant.now().toEpochMilli();
        this.updatedAt = this.settledAt;

        if (this.totalRejectedAmount.compareTo(BigDecimal.ZERO) == 0 && this.totalApprovedAmount.compareTo(this.totalClaimedAmount) >= 0) {
            this.status = Status.PAID;
        } else if (this.totalApprovedAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.status = Status.PARTIALLY_PAID;
        } else {
            this.status = Status.REJECTED;
        }
    }
}
