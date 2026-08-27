package com.bemo.hr.platform.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "webhook_deliveries")
public class WebhookDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "endpoint_id", nullable = false, length = 36)
    private String endpointId;
    @Column(name = "event", nullable = false, length = 50)
    private String event;
    @Column(name = "payload", nullable = false, columnDefinition = "text")
    private String payload;
    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";
    @Column(name = "attempts", nullable = false)
    private int attempts = 0;
    @Column(name = "last_error", length = 1000)
    private String lastError;
    @Column(name = "response_status")
    private Integer responseStatus;
    @Column(name = "created_at", nullable = false)
    private long createdAt;

    protected WebhookDelivery() {}

    public WebhookDelivery(String endpointId, String event, String payload) {
        this.endpointId = endpointId;
        this.event = event;
        this.payload = payload;
        this.status = "PENDING";
        this.attempts = 0;
    }

    public Long getId() { return id; }
    public String getEndpointId() { return endpointId; }
    public String getEvent() { return event; }
    public String getPayload() { return payload; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public Integer getResponseStatus() { return responseStatus; }
    public void setResponseStatus(Integer responseStatus) { this.responseStatus = responseStatus; }
    public long getCreatedAt() { return createdAt; }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); }
}
