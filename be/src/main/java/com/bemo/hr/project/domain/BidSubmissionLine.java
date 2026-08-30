package com.bemo.hr.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Entity
@Table(name = "bid_submission_lines")
public class BidSubmissionLine {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "bidder_id", length = 36, nullable = false)
    private String bidderId;

    @Column(name = "boq_item_id", length = 36, nullable = false)
    private String boqItemId;

    @Column(name = "unit_rate", precision = 18, scale = 4, nullable = false)
    private BigDecimal unitRate;

    @Column(name = "total_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "technical_remarks", length = 500)
    private String technicalRemarks;

    @Column(name = "deviations_notes", length = 500)
    private String deviationsNotes;

    protected BidSubmissionLine() {
    }

    public BidSubmissionLine(String bidderId, String boqItemId, BigDecimal unitRate,
                             BigDecimal quantity, String technicalRemarks, String deviationsNotes) {
        this.id = UUID.randomUUID().toString();
        this.bidderId = bidderId;
        this.boqItemId = boqItemId;
        this.unitRate = unitRate != null ? unitRate : BigDecimal.ZERO;
        BigDecimal qty = quantity != null ? quantity : BigDecimal.ONE;
        this.totalAmount = this.unitRate.multiply(qty);
        this.technicalRemarks = technicalRemarks != null ? technicalRemarks.strip() : null;
        this.deviationsNotes = deviationsNotes != null ? deviationsNotes.strip() : null;
    }
}
