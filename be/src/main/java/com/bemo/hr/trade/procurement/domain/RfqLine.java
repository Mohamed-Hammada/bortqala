package com.bemo.hr.trade.procurement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "rfq_lines")
public class RfqLine {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "rfq_id", nullable = false, length = 36)
    private String rfqId;

    @Column(name = "requisition_line_id", length = 36)
    private String requisitionLineId;

    @Column(name = "item_id", nullable = false, length = 36)
    private String itemId;

    @Column(name = "item_name", nullable = false, length = 255)
    private String itemName;

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal quantity;

    @Column(length = 20)
    private String uom;

    protected RfqLine() {
    }

    public RfqLine(String rfqId, String requisitionLineId, String itemId, String itemName, BigDecimal quantity, String uom) {
        this.id = UUID.randomUUID().toString();
        this.rfqId = rfqId;
        this.requisitionLineId = requisitionLineId;
        this.itemId = itemId;
        this.itemName = itemName;
        this.quantity = quantity;
        this.uom = uom;
    }

    public String getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public String getRfqId() {
        return rfqId;
    }

    public String getRequisitionLineId() {
        return requisitionLineId;
    }

    public String getItemId() {
        return itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public String getUom() {
        return uom;
    }
}
