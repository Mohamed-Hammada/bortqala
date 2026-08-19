package com.bemo.hr.project.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Getter
@Entity
@Table(name = "progress_claim_adjustments")
public class ProgressClaimAdjustment {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "claim_id", length = 36, nullable = false)
    private String claimId;

    @Enumerated(EnumType.STRING)
    @Column(name = "adjustment_type", length = 30, nullable = false)
    private AdjustmentType adjustmentType;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "percentage_rate", precision = 5, scale = 2)
    private BigDecimal percentageRate;

    @Column(name = "calculation_basis_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal calculationBasisAmount;

    @Column(name = "adjustment_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal adjustmentAmount;

    @Column(name = "is_addition", nullable = false)
    private boolean addition;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    protected ProgressClaimAdjustment() {
    }

    public ProgressClaimAdjustment(String claimId, AdjustmentType adjustmentType, String description,
                                   BigDecimal percentageRate, BigDecimal calculationBasisAmount,
                                   BigDecimal fixedAmount, boolean addition, String notes) {
        this.id = UUID.randomUUID().toString();
        this.claimId = claimId;
        this.adjustmentType = adjustmentType != null ? adjustmentType : AdjustmentType.RETENTION;
        this.description = description != null ? description.strip() : this.adjustmentType.name();
        this.percentageRate = percentageRate;
        this.calculationBasisAmount = calculationBasisAmount != null ? calculationBasisAmount : BigDecimal.ZERO;
        this.addition = addition;
        this.notes = notes != null ? notes.strip() : null;

        if (this.percentageRate != null && this.calculationBasisAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.adjustmentAmount = this.calculationBasisAmount.multiply(this.percentageRate)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            this.adjustmentAmount = fixedAmount != null ? fixedAmount : BigDecimal.ZERO;
        }
    }

    public void recalculate(BigDecimal basisAmount) {
        if (basisAmount != null) {
            this.calculationBasisAmount = basisAmount;
            if (this.percentageRate != null) {
                this.adjustmentAmount = this.calculationBasisAmount.multiply(this.percentageRate)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            }
        }
    }
}
