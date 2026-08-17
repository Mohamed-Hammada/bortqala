package com.bemo.hr.product.pack;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_industry_packs")
@Getter
public class TenantIndustryPack {
    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "pack_id", nullable = false)
    private String packId;
    @Column(name = "installed_version", nullable = false)
    private int installedVersion;
    @Column(nullable = false)
    private String status;
    @Column(name = "settings_json", nullable = false, length = 4000)
    private String settingsJson;
    @Column(nullable = false)
    private boolean customized;
    @Column(name = "operation_id", nullable = false, length = 80)
    private String operationId;
    @Column(name = "last_upgrade_operation_id", length = 80)
    private String lastUpgradeOperationId;
    @Column(name = "installed_by", nullable = false)
    private String installedBy;
    @Column(name = "installed_at", nullable = false)
    private Instant installedAt;
    @Version
    private long version;

    protected TenantIndustryPack() {
    }

    public TenantIndustryPack(IndustryPack pack, String operation, String actor, String settings) {
        id = UUID.randomUUID().toString();
        packId = pack.getId();
        installedVersion = pack.getPackVersion();
        status = "INSTALLED";
        settingsJson = settings;
        operationId = operation;
        installedBy = actor;
        installedAt = Instant.now();
    }

    public void customize(String settings) {
        settingsJson = settings;
        customized = true;
    }

    public void upgrade(IndustryPack pack, String defaults, String operation) {
        installedVersion = pack.getPackVersion();
        lastUpgradeOperationId = operation;
        if (!customized) settingsJson = defaults;
    }
}
