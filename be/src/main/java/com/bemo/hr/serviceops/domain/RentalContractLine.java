package com.bemo.hr.serviceops.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "srv_rental_contract_lines")
@Getter
@Setter
public class RentalContractLine {

    @Id
    @Column(length = 36)
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false, length = 50)
    private String appId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private RentalContract contract;

    @Column(name = "rental_item_id", nullable = false, length = 36)
    private String rentalItemId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity;

    @Column(name = "unit_rate", nullable = false, precision = 14, scale = 2)
    private BigDecimal unitRate;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    protected RentalContractLine() {}

    public RentalContractLine(String appId, String rentalItemId, BigDecimal quantity, BigDecimal unitRate) {
        this.id = UUID.randomUUID().toString();
        this.appId = appId;
        this.rentalItemId = rentalItemId;
        this.quantity = quantity != null ? quantity : BigDecimal.ONE;
        this.unitRate = unitRate != null ? unitRate : BigDecimal.ZERO;
        this.totalAmount = this.quantity.multiply(this.unitRate);
    }
}
