package com.bemo.hr.manufacturing.production.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "bom_lines")
public class BomLine {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bom_id", nullable = false)
    private BomHeader bomHeader;

    @Column(name = "component_item_id", nullable = false, length = 36)
    private String componentItemId;

    @Column(name = "component_item_name", nullable = false, length = 255)
    private String componentItemName;

    @Column(name = "quantity_per", nullable = false, precision = 12, scale = 4)
    private BigDecimal quantityPer;

    @Column(name = "unit_of_measure", length = 50)
    private String unitOfMeasure;

    @Column(name = "waste_percent", precision = 5, scale = 2)
    private BigDecimal wastePercent;

    @Column(name = "line_number", nullable = false)
    private int lineNumber;

    protected BomLine() {}

    public BomLine(String componentItemId, String componentItemName, BigDecimal quantityPer,
                   String unitOfMeasure, BigDecimal wastePercent, int lineNumber) {
        this.id = UUID.randomUUID().toString();
        this.componentItemId = componentItemId;
        this.componentItemName = componentItemName;
        this.quantityPer = quantityPer == null ? BigDecimal.ONE : quantityPer;
        this.unitOfMeasure = unitOfMeasure;
        this.wastePercent = wastePercent == null ? BigDecimal.ZERO : wastePercent;
        this.lineNumber = lineNumber;
    }

    void attachTo(BomHeader bomHeader) {
        this.bomHeader = bomHeader;
    }

    public String getId() { return id; }
    public String getBomId() { return bomHeader == null ? null : bomHeader.getId(); }
    public String getComponentItemId() { return componentItemId; }
    public String getComponentItemName() { return componentItemName; }
    public BigDecimal getQuantityPer() { return quantityPer; }
    public String getUnitOfMeasure() { return unitOfMeasure; }
    public BigDecimal getWastePercent() { return wastePercent; }
    public int getLineNumber() { return lineNumber; }
}
