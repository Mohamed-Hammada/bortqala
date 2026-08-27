package com.bemo.hr.recruitment.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "application_stage_events")
public class ApplicationStageEvent {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "application_id", nullable = false)
    private String applicationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_stage", nullable = false, length = 20)
    private ApplicationStage fromStage;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_stage", nullable = false, length = 20)
    private ApplicationStage toStage;

    @Column(name = "actor", nullable = false, length = 100)
    private String actor;

    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "event_at", nullable = false)
    private long eventAt;

    protected ApplicationStageEvent() {
    }

    public ApplicationStageEvent(String applicationId, ApplicationStage fromStage,
                                 ApplicationStage toStage, String actor, String note) {
        this.id = UUID.randomUUID().toString();
        this.applicationId = applicationId;
        this.fromStage = fromStage;
        this.toStage = toStage;
        this.actor = actor;
        this.note = note;
        this.eventAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getApplicationId() { return applicationId; }
    public ApplicationStage getFromStage() { return fromStage; }
    public ApplicationStage getToStage() { return toStage; }
    public String getActor() { return actor; }
    public String getNote() { return note; }
    public long getEventAt() { return eventAt; }
}
