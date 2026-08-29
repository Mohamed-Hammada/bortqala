package com.bemo.hr.workforce;

import com.bemo.hr.shared.security.TenantContext;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Effective-dated daily billing rate charged to a client for a worker category.
 * Rates never overlap for the same client + category; {@code effectiveFrom} is
 * the inclusive start date and {@code effectiveTo} is the inclusive end date
 * (null = still active).
 */
@Entity
@Table(name = "client_worker_rates")
public class ClientWorkerRate {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "app_id", nullable = false, length = 36)
    private String appId;

    @Column(name = "client_party_id", nullable = false, length = 36)
    private String clientPartyId;

    @Column(name = "worker_category_id", nullable = false, length = 36)
    private String workerCategoryId;

    @Column(name = "day_rate", nullable = false, precision = 12, scale = 2)
    private BigDecimal dayRate;

    @Column(name = "effective_from", nullable = false, length = 10)
    private String effectiveFrom;

    @Column(name = "effective_to", length = 10)
    private String effectiveTo;

    @Column(name = "created_by", nullable = false, length = 60)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ClientWorkerRate() {
    }

    public ClientWorkerRate(String id, String clientPartyId, String workerCategoryId,
                            BigDecimal dayRate, String effectiveFrom, String effectiveTo, String createdBy) {
        this.id = id;
        this.appId = TenantContext.currentOrSystem();
        this.clientPartyId = clientPartyId;
        this.workerCategoryId = workerCategoryId;
        this.dayRate = dayRate;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void updateRate(BigDecimal dayRate, String effectiveFrom, String effectiveTo) {
        this.dayRate = dayRate;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getClientPartyId() {
        return clientPartyId;
    }

    public String getWorkerCategoryId() {
        return workerCategoryId;
    }

    public BigDecimal getDayRate() {
        return dayRate;
    }

    public String getEffectiveFrom() {
        return effectiveFrom;
    }

    public String getEffectiveTo() {
        return effectiveTo;
    }

    public long getVersion() {
        return version;
    }
}