package com.bemo.hr.leave.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "leave_requests")
public class LeaveRequest {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "request_number", nullable = false, length = 50)
    private String requestNumber;

    @Column(name = "employee_id", nullable = false, length = 36)
    private String employeeId;

    @Column(name = "leave_type_id", nullable = false, length = 36)
    private String leaveTypeId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "total_days", nullable = false, precision = 6, scale = 2)
    private BigDecimal totalDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private LeaveRequestStatus status;

    @Column(name = "reason", length = 1000)
    private String reason;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Column(name = "approver_user_id", length = 36)
    private String approverUserId;

    @Column(name = "approved_at")
    private Long approvedAt;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    protected LeaveRequest() {
    }

    public LeaveRequest(String requestNumber,
                        String employeeId,
                        String leaveTypeId,
                        LocalDate startDate,
                        LocalDate endDate,
                        BigDecimal totalDays,
                        String reason) {
        this.id = UUID.randomUUID().toString();
        this.requestNumber = requestNumber;
        this.employeeId = employeeId;
        this.leaveTypeId = leaveTypeId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalDays = totalDays;
        this.status = LeaveRequestStatus.PENDING_APPROVAL;
        this.reason = reason;
    }

    public void approve(String approverUserId) {
        this.status = LeaveRequestStatus.APPROVED;
        this.approverUserId = approverUserId;
        this.approvedAt = System.currentTimeMillis();
    }

    public void reject(String rejectionReason) {
        this.status = LeaveRequestStatus.REJECTED;
        this.rejectionReason = rejectionReason;
    }

    public void cancel() {
        this.status = LeaveRequestStatus.CANCELLED;
    }

    @PrePersist
    void prePersist() {
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public String getRequestNumber() {
        return requestNumber;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getLeaveTypeId() {
        return leaveTypeId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public BigDecimal getTotalDays() {
        return totalDays;
    }

    public LeaveRequestStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public String getApproverUserId() {
        return approverUserId;
    }

    public Long getApprovedAt() {
        return approvedAt;
    }

    public long getVersion() {
        return version;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }
}
