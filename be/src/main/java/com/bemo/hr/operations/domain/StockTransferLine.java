package com.bemo.hr.operations.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "stock_transfer_lines")
public class StockTransferLine {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "transfer_id", nullable = false, length = 36)
    private String transferId;

    @Column(name = "item_id", nullable = false, length = 36)
    private String itemId;

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal quantity;

    protected StockTransferLine() {}

    public StockTransferLine(String transferId, String itemId, BigDecimal quantity) {
        this.id = UUID.randomUUID().toString();
        this.transferId = transferId;
        this.itemId = itemId;
        this.quantity = quantity;
    }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getTransferId() { return transferId; }
    public String getItemId() { return itemId; }
    public BigDecimal getQuantity() { return quantity; }
}
