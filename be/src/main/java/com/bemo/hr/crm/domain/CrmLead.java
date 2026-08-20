package com.bemo.hr.crm.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "crm_leads")
public class CrmLead {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "lead_code", length = 50, nullable = false)
    private String leadCode;

    @Column(name = "name", length = 150, nullable = false)
    private String name;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "company_name", length = 150)
    private String companyName;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", length = 30, nullable = false)
    private CrmLeadSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private CrmLeadStatus status;

    @Column(name = "estimated_value", precision = 15, scale = 2, nullable = false)
    private BigDecimal estimatedValue;

    @Column(name = "assigned_sales_agent_id", length = 36)
    private String assignedSalesAgentId;

    @Column(name = "business_party_id", length = 36)
    private String businessPartyId;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    protected CrmLead() {}

    public CrmLead(String leadCode, String name, String phone, String email, String companyName,
                   CrmLeadSource source, CrmLeadStatus status, BigDecimal estimatedValue,
                   String assignedSalesAgentId, String notes) {
        this.id = UUID.randomUUID().toString();
        this.leadCode = leadCode;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.companyName = companyName;
        this.source = source != null ? source : CrmLeadSource.WEBSITE;
        this.status = status != null ? status : CrmLeadStatus.NEW;
        this.estimatedValue = estimatedValue != null ? estimatedValue : BigDecimal.ZERO;
        this.assignedSalesAgentId = assignedSalesAgentId;
        this.notes = notes;
        long now = Instant.now().toEpochMilli();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String name, String phone, String email, String companyName,
                       CrmLeadSource source, CrmLeadStatus status, BigDecimal estimatedValue,
                       String assignedSalesAgentId, String notes) {
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.companyName = companyName;
        if (source != null) this.source = source;
        if (status != null) this.status = status;
        if (estimatedValue != null) this.estimatedValue = estimatedValue;
        this.assignedSalesAgentId = assignedSalesAgentId;
        this.notes = notes;
        this.updatedAt = Instant.now().toEpochMilli();
    }

    public void convertToCustomer(String businessPartyId) {
        this.businessPartyId = businessPartyId;
        this.status = CrmLeadStatus.WON;
        this.updatedAt = Instant.now().toEpochMilli();
    }

    public void setStatus(CrmLeadStatus status) {
        this.status = status;
        this.updatedAt = Instant.now().toEpochMilli();
    }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getLeadCode() { return leadCode; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getCompanyName() { return companyName; }
    public CrmLeadSource getSource() { return source; }
    public CrmLeadStatus getStatus() { return status; }
    public BigDecimal getEstimatedValue() { return estimatedValue; }
    public String getAssignedSalesAgentId() { return assignedSalesAgentId; }
    public String getBusinessPartyId() { return businessPartyId; }
    public String getNotes() { return notes; }
    public Long getVersion() { return version; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
}
