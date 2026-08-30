package com.bemo.hr.trade.procurement.request.domain;

import com.bemo.hr.shared.domain.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "purchase_request_lines")
public class PurchaseRequestLine {

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "request_id", nullable = false, length = 36)
    private String requestId;
    @Column(name = "item_id", nullable = false, length = 36)
    private String itemId;
    @Column(name = "item_name", nullable = false, length = 255)
    private String itemName;
    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal quantity;
    @Column(name = "unit_of_measure", length = 30)
    private String unitOfMeasure;
    @Column(name = "estimated_unit_price", precision = 15, scale = 2)
    private BigDecimal estimatedUnitPrice;
    @Column(name = "converted_quantity", nullable = false, precision = 15, scale = 3)
    private BigDecimal convertedQuantity = BigDecimal.ZERO;
    @Column(name = "created_at", nullable = false)
    private long createdAt;
    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    protected PurchaseRequestLine() {
    }

    public PurchaseRequestLine(String requestId, String itemId, String itemName,
                               BigDecimal quantity, String unitOfMeasure, BigDecimal estimatedUnitPrice) {
        if (quantity == null || quantity.signum() <= 0)
            throw new BusinessRuleException("Purchase request quantities must be greater than zero.",
                    "PR_LINE_QUANTITY_INVALID", HttpStatus.CONFLICT);
        this.id = UUID.randomUUID().toString();
        this.requestId = requestId;
        this.itemId = itemId;
        this.itemName = itemName;
        this.quantity = quantity;
        this.unitOfMeasure = unitOfMeasure == null || unitOfMeasure.isBlank() ? null : unitOfMeasure.strip();
        this.estimatedUnitPrice = estimatedUnitPrice;
    }

    public BigDecimal remainingQuantity() {
        return quantity.subtract(convertedQuantity);
    }

    public void registerConversion(BigDecimal amount) {
        if (amount.signum() <= 0 || convertedQuantity.add(amount).compareTo(quantity) > 0)
            throw new BusinessRuleException("Converting " + amount + " would exceed the requested quantity of "
                    + quantity + " for item " + itemName + ".", "PR_QUANTITY_EXCEEDED", HttpStatus.CONFLICT);
        this.convertedQuantity = convertedQuantity.add(amount);
    }

    @PrePersist
    void prePersist() {
        if (convertedQuantity == null) convertedQuantity = BigDecimal.ZERO;
        createdAt = System.currentTimeMillis();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getRequestId() {
        return requestId;
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

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public BigDecimal getEstimatedUnitPrice() {
        return estimatedUnitPrice;
    }

    public BigDecimal getConvertedQuantity() {
        return convertedQuantity;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }
}
