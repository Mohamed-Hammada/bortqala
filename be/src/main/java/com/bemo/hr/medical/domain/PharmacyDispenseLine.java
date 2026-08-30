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
@Table(name = "pharmacy_dispense_lines")
@Getter
@Setter
@NoArgsConstructor
public class PharmacyDispenseLine {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 64, nullable = false)
    private String appId;

    @Column(name = "dispense_record_id", length = 64, nullable = false)
    private String dispenseRecordId;

    @Column(name = "prescription_line_id", length = 64)
    private String prescriptionLineId;

    @Column(name = "pharmacy_item_id", length = 64, nullable = false)
    private String pharmacyItemId;

    @Column(name = "item_id", length = 64, nullable = false)
    private String itemId;

    @Column(name = "batch_number", length = 64)
    private String batchNumber;

    @Column(name = "expiry_date", length = 16)
    private String expiryDate;

    @Column(name = "quantity_dispensed", precision = 12, scale = 3, nullable = false)
    private BigDecimal quantityDispensed = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    public PharmacyDispenseLine(String dispenseRecordId,
                                String prescriptionLineId,
                                String pharmacyItemId,
                                String itemId,
                                String batchNumber,
                                String expiryDate,
                                BigDecimal quantityDispensed) {
        this.id = UUID.randomUUID().toString();
        this.dispenseRecordId = dispenseRecordId;
        this.prescriptionLineId = prescriptionLineId;
        this.pharmacyItemId = pharmacyItemId;
        this.itemId = itemId;
        this.batchNumber = batchNumber;
        this.expiryDate = expiryDate;
        this.quantityDispensed = quantityDispensed != null ? quantityDispensed : BigDecimal.ZERO;
        this.createdAt = Instant.now().toEpochMilli();
    }
}
