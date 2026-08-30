package com.bemo.hr.trade.export.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "export_shipment_lines")
public class ExportShipmentLine {

    @Id
    private String id;

    @Column(name = "line_order", nullable = false)
    private int lineOrder;

    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;

    @Column(name = "item_code", length = 50)
    private String itemCode;

    @Column(name = "lot_reference", length = 100)
    private String lotReference;

    @Column(name = "quantity", nullable = false, precision = 15, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit_of_measure", length = 20)
    private String unitOfMeasure;

    @Column(name = "net_weight_kg", precision = 15, scale = 3)
    private BigDecimal netWeightKg;

    @Column(name = "gross_weight_kg", precision = 15, scale = 3)
    private BigDecimal grossWeightKg;

    @Column(name = "packages_count")
    private Integer packagesCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    private ExportShipment shipment;

    protected ExportShipmentLine() {
    }

    public ExportShipmentLine(int lineOrder, String itemName, BigDecimal quantity) {
        this.id = UUID.randomUUID().toString();
        this.lineOrder = lineOrder;
        this.itemName = itemName;
        this.quantity = quantity;
    }

    public String getId() { return id; }
    public int getLineOrder() { return lineOrder; }
    public void setLineOrder(int v) { this.lineOrder = v; }
    public String getItemName() { return itemName; }
    public void setItemName(String v) { this.itemName = v; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String v) { this.itemCode = v; }
    public String getLotReference() { return lotReference; }
    public void setLotReference(String v) { this.lotReference = v; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal v) { this.quantity = v; }
    public String getUnitOfMeasure() { return unitOfMeasure; }
    public void setUnitOfMeasure(String v) { this.unitOfMeasure = v; }
    public BigDecimal getNetWeightKg() { return netWeightKg; }
    public void setNetWeightKg(BigDecimal v) { this.netWeightKg = v; }
    public BigDecimal getGrossWeightKg() { return grossWeightKg; }
    public void setGrossWeightKg(BigDecimal v) { this.grossWeightKg = v; }
    public Integer getPackagesCount() { return packagesCount; }
    public void setPackagesCount(Integer v) { this.packagesCount = v; }
    public ExportShipment getShipment() { return shipment; }
    public void setShipment(ExportShipment v) { this.shipment = v; }
}
