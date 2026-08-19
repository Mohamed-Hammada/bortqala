package com.bemo.hr.operations;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "inventory_items")
@Getter
public class InventoryItem {
    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(nullable = false, length = 50)
    private String code;
    @Column(nullable = false, length = 160)
    private String name;
    @Column(name = "item_type", nullable = false, length = 50)
    private String itemType;
    @Column(name = "unit_code", nullable = false, length = 30)
    private String unitCode;
    @Column(name = "category_id", length = 36)
    private String categoryId;
    @Column(name = "uom_id", length = 36)
    private String uomId;
    @Column(nullable = false)
    private boolean active;
    @Column(name = "reorder_point", nullable = false, precision = 15, scale = 4)
    private BigDecimal reorderPoint = BigDecimal.ZERO;
    @Column(name = "reorder_quantity", nullable = false, precision = 15, scale = 4)
    private BigDecimal reorderQuantity = BigDecimal.ZERO;
    @Column(length = 100)
    private String barcode;
    @Column(name = "barcode_aliases", length = 500)
    private String barcodeAliases;
    @Column(name = "tracking_type", length = 30)
    private String trackingType = "NONE";
    @Column(name = "shelf_life_days")
    private Integer shelfLifeDays;
    @Column(name = "is_dead_stock", nullable = false)
    private boolean isDeadStock = false;
    @Version
    private long version;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InventoryItem() {
    }

    public InventoryItem(String code, String name, String itemType, String unitCode) {
        this.id = UUID.randomUUID().toString();
        update(code, name, itemType, unitCode, true);
    }

    public void update(String code, String name, String itemType, String unitCode, boolean active) {
        this.code = normalized(code);
        this.name = name.strip();
        this.itemType = normalized(itemType);
        this.unitCode = normalized(unitCode);
        this.active = active;
    }

    public void assignMasterData(String categoryId, String uomId) {
        this.categoryId = categoryId;
        this.uomId = uomId;
    }

    public void configureReorder(BigDecimal reorderPoint, BigDecimal reorderQuantity) {
        this.reorderPoint = nonNegative(reorderPoint);
        this.reorderQuantity = nonNegative(reorderQuantity);
    }

    public void configureTracking(String barcode, String barcodeAliases, String trackingType, Integer shelfLifeDays, boolean isDeadStock) {
        this.barcode = barcode == null || barcode.isBlank() ? null : barcode.strip();
        this.barcodeAliases = barcodeAliases == null || barcodeAliases.isBlank() ? null : barcodeAliases.strip();
        this.trackingType = trackingType == null || trackingType.isBlank() ? "NONE" : trackingType.strip().toUpperCase(Locale.ROOT);
        this.shelfLifeDays = shelfLifeDays != null && shelfLifeDays > 0 ? shelfLifeDays : null;
        this.isDeadStock = isDeadStock;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    private String normalized(String value) {
        return value.strip().toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    private BigDecimal nonNegative(BigDecimal value) {
        return value == null || value.signum() < 0 ? BigDecimal.ZERO : value;
    }
}
