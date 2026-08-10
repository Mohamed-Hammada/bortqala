package com.bemo.hr.trade.sales.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="customer_receipt_allocations") @Getter
public class CustomerReceiptAllocation {
    @Id private String id;
    @TenantId @Column(name="app_id",nullable=false) private String appId;
    @Column(name="receipt_id",nullable=false,length=36) private String receiptId;
    @Column(name="invoice_id",nullable=false,length=36) private String invoiceId;
    @Column(nullable=false,precision=19,scale=2) private BigDecimal amount;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    protected CustomerReceiptAllocation() { }
    public CustomerReceiptAllocation(String receipt,String invoice,BigDecimal amount){id=UUID.randomUUID().toString();receiptId=receipt;invoiceId=invoice;this.amount=amount;}
    @PrePersist void create(){createdAt=Instant.now();}
}
