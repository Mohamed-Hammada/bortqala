package com.bemo.hr.trade.pos.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pos_transactions")
public class PosTransaction {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "transaction_number", nullable = false, length = 50)
    private String transactionNumber;

    @Column(name = "session_id", nullable = false, length = 36)
    private String sessionId;

    @Column(name = "terminal_id", nullable = false, length = 36)
    private String terminalId;

    @Column(name = "cashier_user_id", nullable = false, length = 36)
    private String cashierUserId;

    @Column(name = "customer_id", length = 36)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private PosTransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PosPaymentMethod paymentMethod;

    @Column(name = "subtotal", nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "tax_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "cash_tendered", precision = 15, scale = 2)
    private BigDecimal cashTendered;

    @Column(name = "change_amount", precision = 15, scale = 2)
    private BigDecimal changeAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PosTransactionStatus status;

    @Column(name = "original_transaction_id", length = 36)
    private String originalTransactionId;

    @Column(name = "client_offline_id", length = 100)
    private String clientOfflineId;

    @Column(name = "reprint_count", nullable = false)
    private int reprintCount = 0;

    @Column(name = "last_reprinted_at")
    private Long lastReprintedAt;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @OneToMany(mappedBy = "transactionId", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PosTransactionLine> lines = new ArrayList<>();

    protected PosTransaction() {
    }

    public PosTransaction(String transactionNumber, String sessionId, String terminalId, String cashierUserId,
                          String customerId, PosTransactionType transactionType, PosPaymentMethod paymentMethod,
                          BigDecimal subtotal, BigDecimal discountAmount, BigDecimal taxAmount, BigDecimal totalAmount,
                          BigDecimal cashTendered, BigDecimal changeAmount, String originalTransactionId, String clientOfflineId) {
        this.id = UUID.randomUUID().toString();
        this.transactionNumber = transactionNumber;
        this.sessionId = sessionId;
        this.terminalId = terminalId;
        this.cashierUserId = cashierUserId;
        this.customerId = customerId;
        this.transactionType = transactionType;
        this.paymentMethod = paymentMethod;
        this.subtotal = subtotal;
        this.discountAmount = discountAmount != null ? discountAmount : BigDecimal.ZERO;
        this.taxAmount = taxAmount != null ? taxAmount : BigDecimal.ZERO;
        this.totalAmount = totalAmount;
        this.cashTendered = cashTendered;
        this.changeAmount = changeAmount != null ? changeAmount : BigDecimal.ZERO;
        this.originalTransactionId = originalTransactionId;
        this.clientOfflineId = clientOfflineId;
        this.status = PosTransactionStatus.COMPLETED;
    }

    public void voidTransaction() {
        this.status = PosTransactionStatus.VOIDED;
    }

    public void markRefunded() {
        this.status = PosTransactionStatus.REFUNDED;
    }

    @PrePersist
    void prePersist() {
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public String getTransactionNumber() {
        return transactionNumber;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getTerminalId() {
        return terminalId;
    }

    public String getCashierUserId() {
        return cashierUserId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public PosTransactionType getTransactionType() {
        return transactionType;
    }

    public PosPaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public BigDecimal getCashTendered() {
        return cashTendered;
    }

    public BigDecimal getChangeAmount() {
        return changeAmount;
    }

    public PosTransactionStatus getStatus() {
        return status;
    }

    public String getOriginalTransactionId() {
        return originalTransactionId;
    }

    public String getClientOfflineId() {
        return clientOfflineId;
    }

    public long getVersion() {
        return version;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public List<PosTransactionLine> getLines() {
        return lines;
    }

    public void recordReprint() {
        this.reprintCount++;
        this.lastReprintedAt = System.currentTimeMillis();
    }

    public int getReprintCount() {
        return reprintCount;
    }

    public Long getLastReprintedAt() {
        return lastReprintedAt;
    }
}
