package com.bemo.hr.project.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Getter
@Entity
@Table(name = "progress_claim_lines")
public class ProgressClaimLine {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "claim_id", length = 36, nullable = false)
    private String claimId;

    @Enumerated(EnumType.STRING)
    @Column(name = "line_type", length = 30, nullable = false)
    private ClaimLineType lineType;

    @Column(name = "wbs_node_id", length = 36)
    private String wbsNodeId;

    @Column(name = "item_code", length = 50, nullable = false)
    private String itemCode;

    @Column(name = "description", length = 500, nullable = false)
    private String description;

    @Column(name = "unit_of_measure", length = 30, nullable = false)
    private String unitOfMeasure;

    @Column(name = "contract_quantity", precision = 18, scale = 4, nullable = false)
    private BigDecimal contractQuantity;

    @Column(name = "unit_rate", precision = 18, scale = 4, nullable = false)
    private BigDecimal unitRate;

    @Column(name = "previous_quantity", precision = 18, scale = 4, nullable = false)
    private BigDecimal previousQuantity;

    @Column(name = "current_quantity", precision = 18, scale = 4, nullable = false)
    private BigDecimal currentQuantity;

    @Column(name = "cumulative_quantity", precision = 18, scale = 4, nullable = false)
    private BigDecimal cumulativeQuantity;

    @Column(name = "previous_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal previousAmount;

    @Column(name = "current_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal currentAmount;

    @Column(name = "cumulative_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal cumulativeAmount;

    @Column(name = "percent_complete", precision = 5, scale = 2, nullable = false)
    private BigDecimal percentComplete;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected ProgressClaimLine() {
    }

    public ProgressClaimLine(String claimId, ClaimLineType lineType, String wbsNodeId,
                             String itemCode, String description, String unitOfMeasure,
                             BigDecimal contractQuantity, BigDecimal unitRate,
                             BigDecimal previousQuantity, BigDecimal currentQuantity,
                             String remarks, int sortOrder) {
        this.id = UUID.randomUUID().toString();
        this.claimId = claimId;
        this.lineType = lineType != null ? lineType : ClaimLineType.BOQ_ITEM;
        this.wbsNodeId = wbsNodeId;
        this.itemCode = itemCode != null ? itemCode.strip() : "ITEM";
        this.description = description != null ? description.strip() : "";
        this.unitOfMeasure = unitOfMeasure != null ? unitOfMeasure.strip() : "PCS";
        this.contractQuantity = contractQuantity != null ? contractQuantity : BigDecimal.ZERO;
        this.unitRate = unitRate != null ? unitRate : BigDecimal.ZERO;
        this.previousQuantity = previousQuantity != null ? previousQuantity : BigDecimal.ZERO;
        this.currentQuantity = currentQuantity != null ? currentQuantity : BigDecimal.ZERO;
        this.remarks = remarks != null ? remarks.strip() : null;
        this.sortOrder = sortOrder;

        recalculateAmounts();
    }

    public void updateMeasurement(BigDecimal currentQuantity, BigDecimal unitRate, String remarks) {
        if (currentQuantity != null) this.currentQuantity = currentQuantity;
        if (unitRate != null) this.unitRate = unitRate;
        this.remarks = remarks != null ? remarks.strip() : null;

        recalculateAmounts();
    }

    public void setPreviousQuantity(BigDecimal previousQuantity) {
        this.previousQuantity = previousQuantity != null ? previousQuantity : BigDecimal.ZERO;
        recalculateAmounts();
    }

    private void recalculateAmounts() {
        this.cumulativeQuantity = this.previousQuantity.add(this.currentQuantity);
        this.previousAmount = this.previousQuantity.multiply(this.unitRate).setScale(2, RoundingMode.HALF_UP);
        this.currentAmount = this.currentQuantity.multiply(this.unitRate).setScale(2, RoundingMode.HALF_UP);
        this.cumulativeAmount = this.cumulativeQuantity.multiply(this.unitRate).setScale(2, RoundingMode.HALF_UP);

        if (this.contractQuantity.compareTo(BigDecimal.ZERO) > 0) {
            this.percentComplete = this.cumulativeQuantity.multiply(BigDecimal.valueOf(100))
                    .divide(this.contractQuantity, 2, RoundingMode.HALF_UP);
        } else {
            this.percentComplete = BigDecimal.ZERO;
        }
    }
}
