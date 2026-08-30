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
@Table(name = "insurance_plans")
@Getter
@Setter
@NoArgsConstructor
public class InsurancePlan {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "payer_id", length = 36, nullable = false)
    private String payerId;

    @Column(name = "name", length = 160, nullable = false)
    private String name;

    @Column(name = "coverage_percent", precision = 5, scale = 2, nullable = false)
    private BigDecimal coveragePercent;

    @Column(name = "copay_flat", precision = 15, scale = 2, nullable = false)
    private BigDecimal copayFlat;

    @Column(name = "annual_limit", precision = 15, scale = 2)
    private BigDecimal annualLimit;

    @Column(name = "exclusions_text", length = 1000)
    private String exclusionsText;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public InsurancePlan(String payerId, String name, BigDecimal coveragePercent, BigDecimal copayFlat,
                         BigDecimal annualLimit, String exclusionsText) {
        this.id = UUID.randomUUID().toString();
        this.payerId = payerId;
        this.name = name;
        this.coveragePercent = coveragePercent != null ? coveragePercent : BigDecimal.valueOf(80);
        this.copayFlat = copayFlat != null ? copayFlat : BigDecimal.ZERO;
        this.annualLimit = annualLimit;
        this.exclusionsText = exclusionsText;
        this.active = true;
        this.createdAt = Instant.now().toEpochMilli();
        this.updatedAt = this.createdAt;
    }
}
