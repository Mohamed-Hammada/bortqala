package com.bemo.hr.trade.sales.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

@Entity @Table(name="customer_invoices") @Getter
public class CustomerInvoice {
    public enum Status { DRAFT, OPEN, PARTIALLY_PAID, PAID, CANCELLED }
    @Id private String id;
    @TenantId @Column(name="app_id",nullable=false) private String appId;
    @Column(name="invoice_number",nullable=false,length=50) private String invoiceNumber;
    @Column(name="customer_id",nullable=false,length=36) private String customerId;
    @Column(name="sales_order_id",length=36) private String salesOrderId;
    @Column(name="invoice_date",nullable=false) private LocalDate invoiceDate;
    @Column(name="due_date",nullable=false) private LocalDate dueDate;
    @Column(name="currency_code",nullable=false,length=10) private String currencyCode;
    @Column(nullable=false,precision=19,scale=2) private BigDecimal amount;
    @Column(name="outstanding_amount",nullable=false,precision=19,scale=2) private BigDecimal outstandingAmount;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Status status;
    @Column(name="issued_by",length=100) private String issuedBy;
    @Column(name="issued_at") private Instant issuedAt;
    @Version private long version;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    protected CustomerInvoice() { }
    public CustomerInvoice(String number,String customerId,String orderId,LocalDate date,LocalDate due,String currency,BigDecimal amount){
        id=UUID.randomUUID().toString();invoiceNumber=number.strip();this.customerId=customerId;salesOrderId=blank(orderId);invoiceDate=date;dueDate=due;
        currencyCode=currency.strip().toUpperCase();this.amount=amount;outstandingAmount=amount;status=Status.DRAFT;
    }
    public void issue(String actor){if(status!=Status.DRAFT)throw new IllegalStateException("Invoice is not draft");status=Status.OPEN;issuedBy=actor;issuedAt=Instant.now();}
    public void allocate(BigDecimal value){if(status!=Status.OPEN&&status!=Status.PARTIALLY_PAID)throw new IllegalStateException("Invoice is not open");
        if(value.signum()<=0||value.compareTo(outstandingAmount)>0)throw new IllegalArgumentException("Allocation exceeds outstanding amount");
        outstandingAmount=outstandingAmount.subtract(value);status=outstandingAmount.signum()==0?Status.PAID:Status.PARTIALLY_PAID;}
    public boolean overdue(LocalDate on){return outstandingAmount.signum()>0&&dueDate.isBefore(on)&&(status==Status.OPEN||status==Status.PARTIALLY_PAID);}
    private static String blank(String value){return value==null||value.isBlank()?null:value.strip();}
    @PrePersist void create(){createdAt=Instant.now();updatedAt=createdAt;} @PreUpdate void update(){updatedAt=Instant.now();}
}
