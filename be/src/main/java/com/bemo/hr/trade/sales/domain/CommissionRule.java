package com.bemo.hr.trade.sales.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;

@Entity
@Table(name = "commission_rules")
@Getter
@NoArgsConstructor
public class CommissionRule {

    public enum Basis { INVOICE_TOTAL, COLLECTED }

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Basis basis;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal percent;

    @Column(name = "min_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal minAmount;

    private boolean active;

    @Column(name = "valid_from")
    private Long validFrom; // epoch millis

    @Column(name = "valid_to")
    private Long validTo; // epoch millis

    @Version
    private Long version;

    public CommissionRule(String id, String appId, String name, Basis basis,
                          BigDecimal percent, BigDecimal minAmount,
                          boolean active, Long validFrom, Long validTo) {
        this.id = id;
        this.appId = appId;
        this.name = name;
        this.basis = basis;
        this.percent = percent;
        this.minAmount = minAmount;
        this.active = active;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }
}
