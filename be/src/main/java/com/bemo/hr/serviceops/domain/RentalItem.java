package com.bemo.hr.serviceops.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "srv_rental_items")
@Getter
@Setter
public class RentalItem {

    public enum Status {
        AVAILABLE,
        RENTED,
        MAINTENANCE,
        RETIRED
    }

    @Id
    @Column(length = 36)
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false, length = 50)
    private String appId;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "name_en", length = 255)
    private String nameEn;

    @Column(length = 100)
    private String category;

    @Column(name = "rate_daily", precision = 14, scale = 2)
    private BigDecimal rateDaily;

    @Column(name = "rate_weekly", precision = 14, scale = 2)
    private BigDecimal rateWeekly;

    @Column(name = "rate_monthly", precision = 14, scale = 2)
    private BigDecimal rateMonthly;

    @Column(name = "deposit_amount", precision = 14, scale = 2)
    private BigDecimal depositAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    protected RentalItem() {}

    public RentalItem(String appId, String code, String name, String nameEn, String category,
                      BigDecimal rateDaily, BigDecimal rateWeekly, BigDecimal rateMonthly, BigDecimal depositAmount) {
        this.id = UUID.randomUUID().toString();
        this.appId = appId;
        this.code = code;
        this.name = name;
        this.nameEn = nameEn;
        this.category = category;
        this.rateDaily = rateDaily != null ? rateDaily : BigDecimal.ZERO;
        this.rateWeekly = rateWeekly != null ? rateWeekly : BigDecimal.ZERO;
        this.rateMonthly = rateMonthly != null ? rateMonthly : BigDecimal.ZERO;
        this.depositAmount = depositAmount != null ? depositAmount : BigDecimal.ZERO;
        this.status = Status.AVAILABLE;
        this.version = 0L;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = System.currentTimeMillis();
    }
}
