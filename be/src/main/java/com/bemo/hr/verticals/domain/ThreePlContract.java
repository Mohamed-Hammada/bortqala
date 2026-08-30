package com.bemo.hr.verticals.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;

@Entity
@Table(name = "three_pl_contracts")
@Getter
@Setter
@NoArgsConstructor
public class ThreePlContract {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "tenant_id", length = 64, nullable = false)
    private String tenantId;

    @Column(name = "contract_code", length = 64, nullable = false)
    private String contractCode;

    @Column(name = "client_name", length = 255, nullable = false)
    private String clientName;

    @Column(name = "warehouse_name", length = 128, nullable = false)
    private String warehouseName;

    @Column(name = "pallet_capacity", nullable = false)
    private int palletCapacity;

    @Column(name = "rate_per_pallet_monthly", precision = 18, scale = 2, nullable = false)
    private BigDecimal ratePerPalletMonthly;

    @Column(name = "handling_in_rate_per_pallet", precision = 18, scale = 2, nullable = false)
    private BigDecimal handlingInRatePerPallet;

    @Column(name = "handling_out_rate_per_pallet", precision = 18, scale = 2, nullable = false)
    private BigDecimal handlingOutRatePerPallet;

    @Column(name = "billing_frequency", length = 32, nullable = false)
    private String billingFrequency;

    @Column(name = "status", length = 32, nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;
}
