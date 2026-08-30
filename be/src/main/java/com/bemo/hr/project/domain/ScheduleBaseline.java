package com.bemo.hr.project.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Getter
@Entity
@Table(name = "schedule_baselines")
public class ScheduleBaseline {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "schedule_id", length = 36, nullable = false)
    private String scheduleId;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "approved_at")
    private Long approvedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    protected ScheduleBaseline() {
    }

    public ScheduleBaseline(String scheduleId, int versionNumber, String name,
                            String approvedBy, String notes) {
        this.id = UUID.randomUUID().toString();
        this.scheduleId = scheduleId;
        this.versionNumber = versionNumber;
        this.name = name != null ? name.strip() : ("Baseline v" + versionNumber);
        this.approvedBy = approvedBy;
        this.approvedAt = System.currentTimeMillis();
        this.notes = notes != null ? notes.strip() : null;
        this.createdAt = this.approvedAt;
    }
}
