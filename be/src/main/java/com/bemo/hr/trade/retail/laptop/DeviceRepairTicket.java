package com.bemo.hr.trade.retail.laptop;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "retail_device_repair_tickets")
public class DeviceRepairTicket {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "ticket_number", length = 50, nullable = false)
    private String ticketNumber;

    @Column(name = "device_id", length = 36)
    private String deviceId;

    @Column(name = "serial_number", length = 100, nullable = false)
    private String serialNumber;

    @Column(name = "customer_name", length = 150, nullable = false)
    private String customerName;

    @Column(name = "customer_phone", length = 40, nullable = false)
    private String customerPhone;

    @Column(name = "reported_issue", length = 500, nullable = false)
    private String reportedIssue;

    @Column(name = "diagnosis", length = 500)
    private String diagnosis;

    @Column(name = "technician_notes", length = 500)
    private String technicianNotes;

    @Column(name = "cost_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal costAmount = BigDecimal.ZERO;

    @Column(name = "charged_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal chargedAmount = BigDecimal.ZERO;

    @Column(name = "status", length = 30, nullable = false)
    private String status = "RECEIVED";

    @Column(name = "is_under_warranty", nullable = false)
    private boolean isUnderWarranty = false;

    @Version
    @Column(name = "version", nullable = false)
    private long version = 0L;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DeviceRepairTicket() {
    }

    public DeviceRepairTicket(String ticketNumber, String deviceId, String serialNumber, String customerName,
                              String customerPhone, String reportedIssue, boolean isUnderWarranty) {
        this.id = UUID.randomUUID().toString();
        this.ticketNumber = Objects.requireNonNull(ticketNumber, "ticketNumber must not be null").strip();
        this.deviceId = deviceId;
        this.serialNumber = Objects.requireNonNull(serialNumber, "serialNumber must not be null").strip();
        this.customerName = Objects.requireNonNull(customerName, "customerName must not be null").strip();
        this.customerPhone = Objects.requireNonNull(customerPhone, "customerPhone must not be null").strip();
        this.reportedIssue = Objects.requireNonNull(reportedIssue, "reportedIssue must not be null").strip();
        this.isUnderWarranty = isUnderWarranty;
        this.status = "RECEIVED";
        this.costAmount = BigDecimal.ZERO;
        this.chargedAmount = isUnderWarranty ? BigDecimal.ZERO : BigDecimal.ZERO;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void updateDiagnosis(String diagnosis, String technicianNotes, BigDecimal costAmount, BigDecimal chargedAmount, String status) {
        this.diagnosis = diagnosis;
        this.technicianNotes = technicianNotes;
        if (costAmount != null) this.costAmount = costAmount;
        if (chargedAmount != null) this.chargedAmount = chargedAmount;
        if (status != null) this.status = status;
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public String getReportedIssue() {
        return reportedIssue;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public String getTechnicianNotes() {
        return technicianNotes;
    }

    public BigDecimal getCostAmount() {
        return costAmount;
    }

    public BigDecimal getChargedAmount() {
        return chargedAmount;
    }

    public String getStatus() {
        return status;
    }

    public boolean isUnderWarranty() {
        return isUnderWarranty;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
