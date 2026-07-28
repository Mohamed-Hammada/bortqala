package com.bemo.hr.operations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "inventory_items")
@Getter
public class InventoryItem {
    @Id private String id;
    @TenantId @Column(name = "app_id", nullable = false) private String appId;
    @Column(nullable = false, length = 50) private String code;
    @Column(nullable = false, length = 160) private String name;
    @Column(name = "item_type", nullable = false, length = 50) private String itemType;
    @Column(name = "unit_code", nullable = false, length = 30) private String unitCode;
    @Column(name = "category_id", length = 36) private String categoryId;
    @Column(name = "uom_id", length = 36) private String uomId;
    @Column(nullable = false) private boolean active;
    @Version private long version;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected InventoryItem() { }

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

    @PrePersist void prePersist() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void preUpdate() { updatedAt = Instant.now(); }
    private String normalized(String value) { return value.strip().toUpperCase(Locale.ROOT).replace(' ', '_'); }
}
