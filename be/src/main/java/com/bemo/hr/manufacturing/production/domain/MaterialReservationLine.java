package com.bemo.hr.manufacturing.production.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "material_reservation_lines")
public class MaterialReservationLine {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "reservation_id", nullable = false, length = 36)
    private String reservationId;

    @Column(name = "item_id", nullable = false, length = 36)
    private String itemId;

    @Column(name = "reserved_quantity", nullable = false, precision = 15, scale = 4)
    private BigDecimal reservedQuantity;

    protected MaterialReservationLine() {
    }

    public MaterialReservationLine(String reservationId, String itemId, BigDecimal reservedQuantity) {
        this.id = UUID.randomUUID().toString();
        this.reservationId = reservationId;
        this.itemId = itemId;
        this.reservedQuantity = reservedQuantity;
    }

    public String getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public String getReservationId() {
        return reservationId;
    }

    public String getItemId() {
        return itemId;
    }

    public BigDecimal getReservedQuantity() {
        return reservedQuantity;
    }
}
