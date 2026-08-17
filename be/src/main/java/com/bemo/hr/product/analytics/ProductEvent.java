package com.bemo.hr.product.analytics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "product_events")
@Getter
public class ProductEvent {
    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "event_name", nullable = false)
    private String eventName;
    @Column(name = "feature_key", nullable = false)
    private String featureKey;
    @Column(name = "properties_json", nullable = false, length = 2000)
    private String propertiesJson;
    @Column(name = "operation_id", nullable = false)
    private String operationId;
    @Column(nullable = false)
    private String actor;
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected ProductEvent() {
    }

    public ProductEvent(String name, String feature, String properties, String operation, String actor) {
        id = UUID.randomUUID().toString();
        eventName = name;
        featureKey = feature;
        propertiesJson = properties;
        operationId = operation;
        this.actor = actor;
        occurredAt = Instant.now();
    }
}
