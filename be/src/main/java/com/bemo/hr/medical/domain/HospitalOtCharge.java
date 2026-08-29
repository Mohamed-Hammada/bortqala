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
@Table(name = "hospital_ot_charges")
@Getter
@Setter
@NoArgsConstructor
public class HospitalOtCharge {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "ot_schedule_id", length = 36, nullable = false)
    private String otScheduleId;

    @Column(name = "item_name", length = 160, nullable = false)
    private String itemName;

    @Column(name = "quantity", precision = 10, scale = 2, nullable = false)
    private BigDecimal quantity;

    @Column(name = "unit_price", precision = 15, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "total_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "charged_at", nullable = false)
    private long chargedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public HospitalOtCharge(String otScheduleId, String itemName, BigDecimal quantity, BigDecimal unitPrice) {
        this.id = UUID.randomUUID().toString();
        this.otScheduleId = otScheduleId;
        this.itemName = itemName;
        this.quantity = quantity != null ? quantity : BigDecimal.ONE;
        this.unitPrice = unitPrice != null ? unitPrice : BigDecimal.ZERO;
        this.totalAmount = this.quantity.multiply(this.unitPrice);
        this.chargedAt = Instant.now().toEpochMilli();
    }
}
