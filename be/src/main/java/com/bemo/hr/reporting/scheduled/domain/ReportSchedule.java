package com.bemo.hr.reporting.scheduled.domain;

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
@Table(name = "report_schedules")
public class ReportSchedule {

    public enum ReportKind { ATTENDANCE, PAYROLL, AR_AGING, CASHFLOW, TRENDS, CUSTOM }
    public enum Channel { EMAIL, WHATSAPP }
    public enum Cadence { DAILY, WEEKLY, MONTHLY }
    public enum LastStatus { SUCCESS, FAILED, SKIPPED_CHANNEL, PENDING }

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false, length = 50)
    private String appId;
    @Column(nullable = false, length = 200)
    private String name;
    @Column(name = "report_kind", nullable = false, length = 20)
    private String reportKind;
    @Column(columnDefinition = "text")
    private String params;
    @Column(nullable = false, length = 20)
    private String channel;
    @Column(columnDefinition = "text")
    private String recipients;
    @Column(nullable = false, length = 20)
    private String cadence;
    @Column(name = "time_of_day", length = 5)
    private String timeOfDay;
    @Column(nullable = false)
    private boolean active;
    @Column(name = "last_run_at")
    private Instant lastRunAt;
    @Column(name = "last_status", length = 20)
    private String lastStatus;
    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;
    @Column(name = "consecutive_failures", nullable = false)
    private int consecutiveFailures;
    @Column(name = "created_at", nullable = false)
    private long createdAt;
    @Column(name = "updated_at", nullable = false)
    private long updatedAt;
    @Version
    private Long version;

    protected ReportSchedule() {}

    public ReportSchedule(String appId, String name, ReportKind reportKind, String params,
                          Channel channel, String recipients, Cadence cadence, String timeOfDay) {
        this.id = UUID.randomUUID().toString();
        this.appId = appId;
        this.name = name;
        this.reportKind = reportKind.name();
        this.params = params;
        this.channel = channel.name();
        this.recipients = recipients;
        this.cadence = cadence.name();
        this.timeOfDay = timeOfDay;
        this.active = true;
        this.consecutiveFailures = 0;
    }

    public void markSuccess() {
        this.lastStatus = LastStatus.SUCCESS.name();
        this.lastRunAt = Instant.now();
        this.consecutiveFailures = 0;
        this.lastError = null;
    }

    public void markFailed(String error) {
        this.lastStatus = LastStatus.FAILED.name();
        this.lastRunAt = Instant.now();
        this.consecutiveFailures++;
        this.lastError = error;
    }

    public void markSkippedChannel() {
        this.lastStatus = LastStatus.SKIPPED_CHANNEL.name();
        this.lastRunAt = Instant.now();
    }

    public void deactivate() { this.active = false; }
    public void activate() { this.active = true; this.consecutiveFailures = 0; }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getName() { return name; }
    public ReportKind getReportKind() { return ReportKind.valueOf(reportKind); }
    public String getParams() { return params; }
    public Channel getChannel() { return Channel.valueOf(channel); }
    public String getRecipients() { return recipients; }
    public Cadence getCadence() { return Cadence.valueOf(cadence); }
    public String getTimeOfDay() { return timeOfDay; }
    public boolean isActive() { return active; }
    public Instant getLastRunAt() { return lastRunAt; }
    public String getLastStatus() { return lastStatus; }
    public String getLastError() { return lastError; }
    public int getConsecutiveFailures() { return consecutiveFailures; }
    public Long getVersion() { return version; }

    public void setName(String name) { this.name = name; }
    public void setReportKind(ReportKind reportKind) { this.reportKind = reportKind.name(); }
    public void setParams(String params) { this.params = params; }
    public void setChannel(Channel channel) { this.channel = channel.name(); }
    public void setRecipients(String recipients) { this.recipients = recipients; }
    public void setCadence(Cadence cadence) { this.cadence = cadence.name(); }
    public void setTimeOfDay(String timeOfDay) { this.timeOfDay = timeOfDay; }
    public void setActive(boolean active) { this.active = active; }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }
}
