package com.bemo.hr.trade.sales.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "customer_receipts")
@Getter
public class CustomerReceipt {
    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "receipt_number", nullable = false, length = 50)
    private String receiptNumber;
    @Column(name = "customer_id", nullable = false, length = 36)
    private String customerId;
    @Column(name = "receipt_date", nullable = false)
    private LocalDate receiptDate;
    @Column(name = "currency_code", nullable = false, length = 10)
    private String currencyCode;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Column(name = "unallocated_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal unallocatedAmount;
    @Column(name = "operation_id", nullable = false, length = 80)
    private String operationId;
    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected CustomerReceipt() {
    }

    public CustomerReceipt(String number, String customerId, LocalDate date, String currency, BigDecimal amount, String operation, String actor) {
        id = UUID.randomUUID().toString();
        receiptNumber = number.strip();
        this.customerId = customerId;
        receiptDate = date;
        currencyCode = currency.strip().toUpperCase();
        this.amount = amount;
        unallocatedAmount = amount;
        operationId = operation;
        createdBy = actor;
    }

    public void allocate(BigDecimal value) {
        if (value.signum() <= 0 || value.compareTo(unallocatedAmount) > 0)
            throw new IllegalArgumentException("Allocation exceeds receipt");
        unallocatedAmount = unallocatedAmount.subtract(value);
    }

    @PrePersist
    void create() {
        createdAt = Instant.now();
    }
}
