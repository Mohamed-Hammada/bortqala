package com.bemo.hr.reportbuilder.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "saved_reports")
public class SavedReport {

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false, length = 50)
    private String appId;
    @Column(nullable = false, length = 200)
    private String name;
    @Column(name = "dataset_code", nullable = false, length = 50)
    private String datasetCode;
    @Column(name = "dataset_version", nullable = false)
    private int datasetVersion;
    @Column(name = "definition", nullable = false, length = 8000)
    private String definition;
    @Column(name = "owner_user_id", length = 100)
    private String ownerUserId;
    @Column(name = "shared_role", length = 50)
    private String sharedRole;
    @Column(name = "created_at", nullable = false)
    private long createdAt;
    @Column(name = "updated_at", nullable = false)
    private long updatedAt;
    @Version
    private Long version;

    protected SavedReport() {}

    public SavedReport(String appId, String name, String datasetCode, int datasetVersion,
                       String definition, String ownerUserId) {
        this.id = UUID.randomUUID().toString();
        this.appId = appId;
        this.name = name;
        this.datasetCode = datasetCode;
        this.datasetVersion = datasetVersion;
        this.definition = definition;
        this.ownerUserId = ownerUserId;
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }
    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getName() { return name; }
    public String getDatasetCode() { return datasetCode; }
    public int getDatasetVersion() { return datasetVersion; }
    public String getDefinition() { return definition; }
    public String getOwnerUserId() { return ownerUserId; }
    public String getSharedRole() { return sharedRole; }
    public Long getVersion() { return version; }
    public long getCreatedAt() { return createdAt; }
    public void setName(String n) { this.name = n; }
    public void setDefinition(String d) { this.definition = d; }
    public void setSharedRole(String r) { this.sharedRole = r; }
}
