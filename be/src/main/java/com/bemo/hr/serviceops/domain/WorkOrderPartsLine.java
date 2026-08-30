package com.bemo.hr.serviceops.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "srv_work_order_parts_lines")
@Getter
@Setter
public class WorkOrderPartsLine {

    @Id
    @Column(length = 36)
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false, length = 50)
    private String appId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @Column(name = "item_code", nullable = false, length = 50)
    private String itemCode;

    @Column(name = "item_name", nullable = false, length = 255)
    private String itemName;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    protected WorkOrderPartsLine() {}

    public WorkOrderPartsLine(String appId, String itemCode, String itemName, BigDecimal quantity, BigDecimal unitPrice) {
        this.id = UUID.randomUUID().toString();
        this.appId = appId;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.quantity = quantity != null ? quantity : BigDecimal.ONE;
        this.unitPrice = unitPrice != null ? unitPrice : BigDecimal.ZERO;
        this.totalAmount = this.quantity.multiply(this.unitPrice);
    }
}
