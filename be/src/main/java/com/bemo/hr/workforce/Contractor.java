package com.bemo.hr.workforce;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "contractors")
@Getter
public class Contractor {
    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(nullable = false, length = 50)
    private String code;
    @Column(nullable = false, length = 160)
    private String name;
    @Column(name = "trade_name", length = 160)
    private String tradeName;
    @Column(nullable = false, length = 50)
    private String phone;
    @Column(name = "secondary_phone", length = 50)
    private String secondaryPhone;
    @Column(name = "tax_id", length = 50)
    private String taxId;
    @Column(length = 255)
    private String address;
    @Column(name = "accounting_model", nullable = false, length = 50)
    private String accountingModel;
    @Column(name = "payment_routing", nullable = false, length = 50)
    private String paymentRouting;
    @Column(name = "settlement_cycle_days", nullable = false)
    private int settlementCycleDays;
    @Column(name = "default_daily_rate", precision = 12, scale = 2)
    private BigDecimal defaultDailyRate;
    @Column(name = "fee_type", length = 30)
    private String feeType;
    @Column(name = "fee_value", precision = 12, scale = 2)
    private BigDecimal feeValue;
    @Column(name = "fee_base", length = 30)
    private String feeBase;
    @Column(name = "fixed_period_amount", precision = 12, scale = 2)
    private BigDecimal fixedPeriodAmount;
    @Column(nullable = false, length = 30)
    private String status;
    @Column(length = 1000)
    private String notes;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Contractor() {
    }

    public Contractor(String code, String name, String tradeName, String phone, String secondaryPhone,
                      String taxId, String address, String accountingModel, String paymentRouting,
                      int settlementCycleDays, BigDecimal defaultDailyRate, String feeType,
                      BigDecimal feeValue, String feeBase, BigDecimal fixedPeriodAmount,
                      String status, String notes) {
        this.id = UUID.randomUUID().toString();
        update(code, name, tradeName, phone, secondaryPhone, taxId, address, accountingModel,
                paymentRouting, settlementCycleDays, defaultDailyRate, feeType, feeValue, feeBase,
                fixedPeriodAmount, status, notes);
    }

    public void update(String code, String name, String tradeName, String phone, String secondaryPhone,
                       String taxId, String address, String accountingModel, String paymentRouting,
                       int settlementCycleDays, BigDecimal defaultDailyRate, String feeType,
                       BigDecimal feeValue, String feeBase, BigDecimal fixedPeriodAmount,
                       String status, String notes) {
        this.code = code != null ? code.strip().toUpperCase() : "CTR-" + UUID.randomUUID().toString().substring(0, 6);
        this.name = name != null ? name.strip() : "";
        this.tradeName = nullable(tradeName);
        this.phone = phone != null ? phone.strip() : "";
        this.secondaryPhone = nullable(secondaryPhone);
        this.taxId = nullable(taxId);
        this.address = nullable(address);
        this.accountingModel = accountingModel != null ? accountingModel.strip().toLowerCase() : "worker_net_total";
        this.paymentRouting = paymentRouting != null ? paymentRouting.strip().toLowerCase() : "contractor_full";
        this.settlementCycleDays = settlementCycleDays > 0 ? settlementCycleDays : 15;
        this.defaultDailyRate = defaultDailyRate != null ? defaultDailyRate : BigDecimal.ZERO;
        this.feeType = feeType != null ? feeType.strip().toLowerCase() : "fixed";
        this.feeValue = feeValue != null ? feeValue : BigDecimal.ZERO;
        this.feeBase = feeBase != null ? feeBase.strip().toLowerCase() : "gross";
        this.fixedPeriodAmount = fixedPeriodAmount != null ? fixedPeriodAmount : BigDecimal.ZERO;
        this.status = status != null ? status.strip().toUpperCase() : "ACTIVE";
        this.notes = nullable(notes);
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    private String nullable(String val) {
        return val == null || val.isBlank() ? null : val.strip();
    }
}
