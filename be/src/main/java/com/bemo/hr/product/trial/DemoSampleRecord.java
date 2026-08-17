package com.bemo.hr.product.trial;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "demo_sample_records")
@Getter
public class DemoSampleRecord {
    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "template_code", nullable = false, length = 80)
    private String templateCode;
    @Column(name = "template_version", nullable = false)
    private int templateVersion;
    @Column(name = "record_key", nullable = false, length = 100)
    private String recordKey;
    @Column(name = "payload_json", nullable = false, length = 2000)
    private String payloadJson;
    @Column(name = "reset_operation_id", nullable = false, length = 80)
    private String resetOperationId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DemoSampleRecord() {
    }

    DemoSampleRecord(String templateCode, int version, String key, String payload, String operationId, Instant at) {
        id = UUID.randomUUID().toString();
        this.templateCode = templateCode;
        templateVersion = version;
        recordKey = key;
        payloadJson = payload;
        resetOperationId = operationId;
        createdAt = at;
    }
}
