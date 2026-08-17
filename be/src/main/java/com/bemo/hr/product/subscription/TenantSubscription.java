package com.bemo.hr.product.subscription;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_subscriptions")
@Getter
public class TenantSubscription {
    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false, unique = true)
    private String appId;
    @Column(name = "plan_code", nullable = false, length = 40)
    private String planCode;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;
    @Column(name = "renews_at")
    private Instant renewsAt;
    @Column(name = "ends_at")
    private Instant endsAt;
    @Column(name = "last_operation_id", nullable = false, length = 80)
    private String lastOperationId;
    @Column(name = "updated_by", nullable = false, length = 100)
    private String updatedBy;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    private long version;

    protected TenantSubscription() {
    }

    public TenantSubscription(String plan, String status, Instant starts, Instant renews, Instant ends, String operation, String actor) {
        id = UUID.randomUUID().toString();
        change(plan, status, starts, renews, ends, operation, actor);
    }

    public void change(String plan, String status, Instant starts, Instant renews, Instant ends, String operation, String actor) {
        planCode = plan;
        this.status = status;
        startsAt = starts;
        renewsAt = renews;
        endsAt = ends;
        lastOperationId = operation;
        updatedBy = actor;
        updatedAt = Instant.now();
    }
}
