package com.bemo.hr.trade.sales.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="ar_customer_credit_profiles") @Getter
public class CustomerCreditProfile {
    @Id private String id;
    @TenantId @Column(name="app_id",nullable=false) private String appId;
    @Column(name="customer_id",nullable=false,length=36) private String customerId;
    @Column(name="credit_limit",nullable=false,precision=19,scale=2) private BigDecimal creditLimit;
    @Column(name="payment_terms_days",nullable=false) private int paymentTermsDays;
    @Column(name="credit_hold",nullable=false) private boolean creditHold;
    @Version private long version;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    protected CustomerCreditProfile() { }
    public CustomerCreditProfile(String customerId){id=UUID.randomUUID().toString();this.customerId=customerId;creditLimit=BigDecimal.ZERO;paymentTermsDays=30;}
    public void update(BigDecimal limit,int terms,boolean hold){creditLimit=limit;paymentTermsDays=terms;creditHold=hold;}
    @PrePersist void create(){createdAt=Instant.now();updatedAt=createdAt;} @PreUpdate void update(){updatedAt=Instant.now();}
}
