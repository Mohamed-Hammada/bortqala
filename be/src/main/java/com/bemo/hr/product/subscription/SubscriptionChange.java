package com.bemo.hr.product.subscription;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="subscription_changes") @Getter
public class SubscriptionChange {
    @Id private String id;
    @TenantId @Column(name="app_id",nullable=false) private String appId;
    @Column(name="from_plan",length=40) private String fromPlan;
    @Column(name="to_plan",nullable=false,length=40) private String toPlan;
    @Column(name="from_status",length=20) private String fromStatus;
    @Column(name="to_status",nullable=false,length=20) private String toStatus;
    @Column(nullable=false,length=500) private String reason;
    @Column(name="operation_id",nullable=false,length=80) private String operationId;
    @Column(nullable=false,length=100) private String actor;
    @Column(name="changed_at",nullable=false) private Instant changedAt;
    protected SubscriptionChange() {}
    public SubscriptionChange(String fromPlan,String toPlan,String fromStatus,String toStatus,String reason,String operationId,String actor){id=UUID.randomUUID().toString();this.fromPlan=fromPlan;this.toPlan=toPlan;this.fromStatus=fromStatus;this.toStatus=toStatus;this.reason=reason.strip();this.operationId=operationId;this.actor=actor;changedAt=Instant.now();}
}
