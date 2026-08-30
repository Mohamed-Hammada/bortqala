package com.bemo.hr.serviceops.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "srv_rental_contracts")
@Getter
@Setter
public class RentalContract {

    public enum Status {
        DRAFT,
        ACTIVE,
        CLOSED,
        CANCELLED
    }

    public enum RateUnit {
        DAY,
        WEEK,
        MONTH
    }

    @Id
    @Column(length = 36)
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false, length = 50)
    private String appId;

    @Column(name = "contract_no", nullable = false, length = 50)
    private String contractNo;

    @Column(name = "customer_party_id", nullable = false, length = 36)
    private String customerPartyId;

    @Column(name = "start_date", nullable = false, length = 20)
    private String startDate;

    @Column(name = "expected_end_date", nullable = false, length = 20)
    private String expectedEndDate;

    @Column(name = "actual_end_date", length = 20)
    private String actualEndDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "rate_unit", nullable = false, length = 20)
    private RateUnit rateUnit;

    @Column(name = "rate_amount", precision = 14, scale = 2)
    private BigDecimal rateAmount;

    @Column(name = "deposit_amount", precision = 14, scale = 2)
    private BigDecimal depositAmount;

    @Column(name = "damage_fee", precision = 14, scale = 2)
    private BigDecimal damageFee;

    @Column(name = "total_amount", precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status;

    @Column(name = "invoice_id", length = 36)
    private String invoiceId;

    @Column(length = 1000)
    private String notes;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RentalContractLine> lines = new ArrayList<>();

    protected RentalContract() {}

    public RentalContract(String appId, String contractNo, String customerPartyId,
                          String startDate, String expectedEndDate, RateUnit rateUnit,
                          BigDecimal rateAmount, BigDecimal depositAmount, String notes) {
        this.id = UUID.randomUUID().toString();
        this.appId = appId;
        this.contractNo = contractNo;
        this.customerPartyId = customerPartyId;
        this.startDate = startDate;
        this.expectedEndDate = expectedEndDate;
        this.rateUnit = rateUnit != null ? rateUnit : RateUnit.DAY;
        this.rateAmount = rateAmount != null ? rateAmount : BigDecimal.ZERO;
        this.depositAmount = depositAmount != null ? depositAmount : BigDecimal.ZERO;
        this.damageFee = BigDecimal.ZERO;
        this.totalAmount = BigDecimal.ZERO;
        this.status = Status.DRAFT;
        this.notes = notes;
        this.version = 0L;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void addLine(RentalContractLine line) {
        line.setContract(this);
        this.lines.add(line);
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = System.currentTimeMillis();
    }
}
