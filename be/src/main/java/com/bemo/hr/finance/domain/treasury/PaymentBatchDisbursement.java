package com.bemo.hr.finance.domain.treasury;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "payment_batch_disbursements")
public class PaymentBatchDisbursement {
    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "batch_id", nullable = false, length = 36)
    private String batchId;
    @Column(name = "item_id", nullable = false, length = 36)
    private String itemId;
    @Column(name = "source_document_id", nullable = false, length = 36)
    private String sourceDocumentId;
    @Column(name = "payee_id", nullable = false, length = 36)
    private String payeeId;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;
    @Column(name = "bank_account", length = 100)
    private String bankAccount;
    @Column(name = "operation_id", nullable = false, length = 100)
    private String operationId;
    @Column(name = "created_at", nullable = false)
    private long createdAt;

    protected PaymentBatchDisbursement() {
    }

    public PaymentBatchDisbursement(String batchId, String itemId, String sourceDocumentId, String payeeId, BigDecimal amount, String bankAccount, String operationId) {
        id = UUID.randomUUID().toString();
        this.batchId = batchId;
        this.itemId = itemId;
        this.sourceDocumentId = sourceDocumentId;
        this.payeeId = payeeId;
        this.amount = amount;
        this.bankAccount = bankAccount;
        this.operationId = operationId;
    }

    @PrePersist
    void prePersist() {
        createdAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getBatchId() {
        return batchId;
    }

    public String getItemId() {
        return itemId;
    }

    public String getSourceDocumentId() {
        return sourceDocumentId;
    }

    public String getPayeeId() {
        return payeeId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getBankAccount() {
        return bankAccount;
    }

    public String getOperationId() {
        return operationId;
    }
}
