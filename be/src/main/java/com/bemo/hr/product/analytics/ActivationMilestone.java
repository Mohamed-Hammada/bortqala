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
@Table(name = "activation_milestones")
@Getter
public class ActivationMilestone {
    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "milestone_key", nullable = false)
    private String milestoneKey;
    @Column(name = "source_event_id", nullable = false)
    private String sourceEventId;
    @Column(name = "achieved_at", nullable = false)
    private Instant achievedAt;

    protected ActivationMilestone() {
    }

    public ActivationMilestone(String key, String source, Instant at) {
        id = UUID.randomUUID().toString();
        milestoneKey = key;
        sourceEventId = source;
        achievedAt = at;
    }
}
