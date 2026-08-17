package com.bemo.hr.manufacturing.production.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "material_issue_lines")
public class MaterialIssueLine {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "issue_id", nullable = false, length = 36)
    private String issueId;

    @Column(name = "item_id", nullable = false, length = 36)
    private String itemId;

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal quantity;

    @Column(name = "warehouse_id", nullable = false, length = 36)
    private String warehouseId;

    protected MaterialIssueLine() {
    }

    public MaterialIssueLine(String issueId, String itemId, BigDecimal quantity, String warehouseId) {
        this.id = UUID.randomUUID().toString();
        this.issueId = issueId;
        this.itemId = itemId;
        this.quantity = quantity;
        this.warehouseId = warehouseId;
    }

    public String getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public String getIssueId() {
        return issueId;
    }

    public String getItemId() {
        return itemId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public String getWarehouseId() {
        return warehouseId;
    }
}
