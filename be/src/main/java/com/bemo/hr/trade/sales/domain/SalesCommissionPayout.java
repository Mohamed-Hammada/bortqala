package com.bemo.hr.trade.sales.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sales_commission_payouts", uniqueConstraints = {
        @UniqueConstraint(name = "uq_sales_commission_payout_app_rep_period", columnNames = {"app_id", "rep_id", "period"})
})
@Getter
public class SalesCommissionPayout {
    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "rep_id", nullable = false, length = 36)
    private String repId;
    @Column(nullable = false, length = 7)
    private String period;
    @Column(name = "total_commission", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalCommission;
    @Column(name = "sent_by", nullable = false, length = 100)
    private String sentBy;
    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Version
    @Column(nullable = false)
    private long version;

    protected SalesCommissionPayout() {
    }

    public SalesCommissionPayout(String id, String appId, String repId, String period,
                                 BigDecimal totalCommission, String sentBy) {
        this.id = id;
        this.appId = appId;
        this.repId = repId;
        this.period = period;
        this.totalCommission = totalCommission;
        this.sentBy = sentBy;
        this.sentAt = Instant.now();
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}