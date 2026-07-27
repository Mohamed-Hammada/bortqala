package com.bemo.hr.finance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "currencies")
public class Currency {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(nullable = false, length = 10)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 10)
    private String symbol;

    @Column(name = "is_base", nullable = false)
    private boolean isBase;

    @Column(name = "exchange_rate", nullable = false, precision = 12, scale = 4)
    private BigDecimal exchangeRate;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    protected Currency() {}

    public Currency(String code, String name, String symbol, boolean isBase, BigDecimal exchangeRate, boolean active) {
        this.id = UUID.randomUUID().toString();
        update(code, name, symbol, isBase, exchangeRate, active);
    }

    public void update(String code, String name, String symbol, boolean isBase, BigDecimal exchangeRate, boolean active) {
        this.code = code.strip().toUpperCase();
        this.name = name.strip();
        this.symbol = symbol.strip();
        this.isBase = isBase;
        this.exchangeRate = exchangeRate == null ? BigDecimal.ONE : exchangeRate;
        this.active = active;
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getSymbol() { return symbol; }
    public boolean isBase() { return isBase; }
    public BigDecimal getExchangeRate() { return exchangeRate; }
    public boolean isActive() { return active; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
}
