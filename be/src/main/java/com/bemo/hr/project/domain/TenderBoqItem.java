package com.bemo.hr.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Entity
@Table(name = "tender_boq_items")
public class TenderBoqItem {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "tender_id", length = 36, nullable = false)
    private String tenderId;

    @Column(name = "item_code", length = 50, nullable = false)
    private String itemCode;

    @Column(name = "description", length = 500, nullable = false)
    private String description;

    @Column(name = "description_en", length = 500)
    private String descriptionEn;

    @Column(name = "unit_of_measure", length = 30, nullable = false)
    private String unitOfMeasure;

    @Column(name = "quantity", precision = 18, scale = 4, nullable = false)
    private BigDecimal quantity;

    @Column(name = "estimated_rate", precision = 18, scale = 4, nullable = false)
    private BigDecimal estimatedRate;

    @Column(name = "estimated_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal estimatedAmount;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected TenderBoqItem() {
    }

    public TenderBoqItem(String tenderId, String itemCode, String description,
                         String descriptionEn, String unitOfMeasure, BigDecimal quantity,
                         BigDecimal estimatedRate, int sortOrder) {
        this.id = UUID.randomUUID().toString();
        this.tenderId = tenderId;
        this.itemCode = itemCode != null ? itemCode.strip() : "ITEM";
        this.description = description != null ? description.strip() : "";
        this.descriptionEn = descriptionEn != null ? descriptionEn.strip() : null;
        this.unitOfMeasure = unitOfMeasure != null ? unitOfMeasure.strip() : "PCS";
        this.quantity = quantity != null ? quantity : BigDecimal.ONE;
        this.estimatedRate = estimatedRate != null ? estimatedRate : BigDecimal.ZERO;
        this.estimatedAmount = this.quantity.multiply(this.estimatedRate);
        this.sortOrder = sortOrder;
    }

    public void update(String itemCode, String description, String descriptionEn,
                       String unitOfMeasure, BigDecimal quantity, BigDecimal estimatedRate,
                       int sortOrder) {
        if (itemCode != null && !itemCode.isBlank()) this.itemCode = itemCode.strip();
        if (description != null && !description.isBlank()) this.description = description.strip();
        this.descriptionEn = descriptionEn != null ? descriptionEn.strip() : null;
        if (unitOfMeasure != null && !unitOfMeasure.isBlank()) this.unitOfMeasure = unitOfMeasure.strip();
        if (quantity != null) this.quantity = quantity;
        if (estimatedRate != null) this.estimatedRate = estimatedRate;
        this.estimatedAmount = this.quantity.multiply(this.estimatedRate);
        this.sortOrder = sortOrder;
    }
}
