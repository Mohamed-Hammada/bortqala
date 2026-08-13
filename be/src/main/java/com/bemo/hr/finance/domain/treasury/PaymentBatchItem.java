package com.bemo.hr.finance.domain.treasury;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "payment_batch_items")
public class PaymentBatchItem {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "batch_id", nullable = false, length = 36)
    private String batchId;

    @Column(name = "document_id", nullable = false, length = 36)
    private String documentId;

    @Column(name = "payee_id", nullable = false, length = 36)
    private String payeeId;

    @Column(name = "payee_name", nullable = false, length = 255)
    private String payeeName;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "bank_account", length = 100)
    private String bankAccount;

    @Column(name = "disbursement_id", length = 36) private String disbursementId;

    protected PaymentBatchItem() {}

    public PaymentBatchItem(String batchId, String documentId, String payeeId, String payeeName, BigDecimal amount, String bankAccount) {
        this.id = UUID.randomUUID().toString();
        this.batchId = batchId;
        this.documentId = documentId;
        this.payeeId = payeeId;
        this.payeeName = payeeName;
        this.amount = amount;
        this.bankAccount = bankAccount;
    }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getBatchId() { return batchId; }
    public String getDocumentId() { return documentId; }
    public String getPayeeId() { return payeeId; }
    public String getPayeeName() { return payeeName; }
    public BigDecimal getAmount() { return amount; }
    public String getBankAccount() { return bankAccount; }
    public String getDisbursementId() { return disbursementId; }
    public void linkDisbursement(String id) { if (disbursementId == null) disbursementId = id; }
}
