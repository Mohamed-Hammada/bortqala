package com.bemo.hr.automation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "recurring_templates")
public class RecurringTemplate {

    public enum Kind { PO, INVOICE, JOURNAL }
    public enum Cadence { MONTHLY, WEEKLY, CUSTOM_DAYS }

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false, length = 50)
    private String appId;
    @Column(nullable = false, length = 20)
    private String kind;
    @Column(name = "template_name", nullable = false, length = 200)
    private String templateName;
    @Column(name = "payload_snapshot", columnDefinition = "text", nullable = false)
    private String payloadSnapshot;
    @Column(nullable = false, length = 20)
    private String cadence;
    @Column(name = "cadence_days")
    private Integer cadenceDays;
    @Column(name = "next_run_at", nullable = false)
    private Instant nextRunAt;
    @Column(nullable = false)
    private boolean active;
    @Column(name = "last_created_ref", length = 36)
    private String lastCreatedRef;
    @Column(name = "created_at", nullable = false)
    private long createdAt;
    @Column(name = "updated_at", nullable = false)
    private long updatedAt;
    @Version
    private Long version;

    protected RecurringTemplate() {}

    public RecurringTemplate(String appId, Kind kind, String templateName, String payloadSnapshot,
                             Cadence cadence, Integer cadenceDays, Instant nextRunAt) {
        this.id = UUID.randomUUID().toString();
        this.appId = appId;
        this.kind = kind.name();
        this.templateName = templateName;
        this.payloadSnapshot = payloadSnapshot;
        this.cadence = cadence.name();
        this.cadenceDays = cadenceDays;
        this.nextRunAt = nextRunAt;
        this.active = true;
    }

    public void advanceNextRun() {
        if (cadence == Cadence.MONTHLY.name()) {
            nextRunAt = nextRunAt.plusSeconds(30L * 86400);
        } else if (cadence == Cadence.WEEKLY.name()) {
            nextRunAt = nextRunAt.plusSeconds(7L * 86400);
        } else if (cadenceDays != null && cadenceDays > 0) {
            nextRunAt = nextRunAt.plusSeconds((long) cadenceDays * 86400);
        }
    }

    public void deactivate() { this.active = false; }
    public void activate() { this.active = true; }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public Kind getKind() { return Kind.valueOf(kind); }
    public String getTemplateName() { return templateName; }
    public String getPayloadSnapshot() { return payloadSnapshot; }
    public Cadence getCadence() { return Cadence.valueOf(cadence); }
    public Integer getCadenceDays() { return cadenceDays; }
    public Instant getNextRunAt() { return nextRunAt; }
    public boolean isActive() { return active; }
    public String getLastCreatedRef() { return lastCreatedRef; }
    public void setLastCreatedRef(String ref) { this.lastCreatedRef = ref; }
    public Long getVersion() { return version; }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }
}
