package com.bemo.hr.workforce;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "labor_request_items")
@Getter
public class LaborRequestItem {
    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "request_id", nullable = false, length = 36)
    private String requestId;
    @Column(name = "category_id", nullable = false, length = 36)
    private String categoryId;
    @Column(name = "requested_count", nullable = false)
    private int requestedCount;
    @Column(name = "sent_count", nullable = false)
    private int sentCount;
    @Column(name = "accepted_count", nullable = false)
    private int acceptedCount;
    @Column(name = "variance_count", nullable = false)
    private int varianceCount;

    protected LaborRequestItem() {
    }

    public LaborRequestItem(String requestId, String categoryId, int requestedCount, int sentCount, int acceptedCount) {
        this.id = UUID.randomUUID().toString();
        this.requestId = requestId;
        this.categoryId = categoryId;
        this.requestedCount = Math.max(0, requestedCount);
        this.sentCount = Math.max(0, sentCount);
        this.acceptedCount = Math.max(0, acceptedCount);
        this.varianceCount = this.requestedCount - this.acceptedCount;
    }

    public void updateCounts(int sentCount, int acceptedCount) {
        this.sentCount = Math.max(0, sentCount);
        this.acceptedCount = Math.max(0, acceptedCount);
        this.varianceCount = this.requestedCount - this.acceptedCount;
    }
}
