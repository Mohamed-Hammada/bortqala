package com.bemo.hr.product.trial;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;

@Entity
@Table(name = "demo_tenant_templates")
@Getter
public class DemoTenantTemplate {
    @Id
    private String id;
    @Column(nullable = false, length = 80)
    private String code;
    @Column(name = "template_version", nullable = false)
    private int templateVersion;
    @Column(name = "name_key", nullable = false, length = 120)
    private String nameKey;
    @Column(name = "sample_json", nullable = false, length = 4000)
    private String sampleJson;
    @Column(nullable = false)
    private boolean active;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DemoTenantTemplate() {
    }

    DemoTenantTemplate(String id, String code, int version, String sampleJson) {
        this.id = id;
        this.code = code;
        templateVersion = version;
        this.sampleJson = sampleJson;
        nameKey = "trialDemo.template.contractorWorkforce";
        active = true;
        createdAt = Instant.now();
    }
}
