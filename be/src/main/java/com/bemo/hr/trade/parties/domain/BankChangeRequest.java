package com.bemo.hr.trade.parties.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "bank_change_requests")
public class BankChangeRequest {

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Enumerated(EnumType.STRING)
    @Column(name = "party_type", nullable = false, length = 30)
    private PartyType partyType;
    @Column(name = "party_id", nullable = false, length = 36)
    private String partyId;
    @Column(name = "old_iban", length = 50)
    private String oldIban;
    @Column(name = "new_iban", nullable = false, length = 50)
    private String newIban;
    @Column(name = "old_bank_name", length = 100)
    private String oldBankName;
    @Column(name = "new_bank_name", nullable = false, length = 100)
    private String newBankName;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;
    @Column(name = "requested_by", nullable = false, length = 100)
    private String requestedBy;
    @Column(name = "approved_by", length = 100)
    private String approvedBy;
    @Column(length = 500)
    private String reason;
    @Column(name = "created_at", nullable = false)
    private long createdAt;
    @Column(name = "updated_at", nullable = false)
    private long updatedAt;
    @Version
    @Column(nullable = false)
    private long version;

    protected BankChangeRequest() {
    }

    public BankChangeRequest(PartyType partyType, String partyId, String oldIban, String newIban, String oldBankName, String newBankName, String reason, String requestedBy) {
        this.id = UUID.randomUUID().toString();
        this.partyType = partyType;
        this.partyId = partyId;
        this.oldIban = oldIban;
        this.newIban = newIban.strip();
        this.oldBankName = oldBankName;
        this.newBankName = newBankName.strip();
        this.reason = reason == null ? null : reason.strip();
        this.requestedBy = requestedBy;
        this.status = Status.PENDING;
    }

    public void approve(String approverUsername) {
        if (this.status != Status.PENDING) {
            throw new IllegalStateException("Only PENDING requests can be approved");
        }
        this.status = Status.APPROVED;
        this.approvedBy = approverUsername;
    }

    public void reject(String approverUsername, String rejectionReason) {
        if (this.status != Status.PENDING) {
            throw new IllegalStateException("Only PENDING requests can be rejected");
        }
        this.status = Status.REJECTED;
        this.approvedBy = approverUsername;
        if (rejectionReason != null && !rejectionReason.isBlank()) {
            this.reason = (this.reason == null ? "" : this.reason + " | Rejection: ") + rejectionReason.strip();
        }
    }

    @PrePersist
    void prePersist() {
        createdAt = System.currentTimeMillis();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public PartyType getPartyType() {
        return partyType;
    }

    public String getPartyId() {
        return partyId;
    }

    public String getOldIban() {
        return oldIban;
    }

    public String getNewIban() {
        return newIban;
    }

    public String getOldBankName() {
        return oldBankName;
    }

    public String getNewBankName() {
        return newBankName;
    }

    public Status getStatus() {
        return status;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public String getReason() {
        return reason;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }

    public enum PartyType {
        SUPPLIER, WORKFORCE_CONTRACTOR
    }

    public enum Status {
        PENDING, APPROVED, REJECTED
    }
}
