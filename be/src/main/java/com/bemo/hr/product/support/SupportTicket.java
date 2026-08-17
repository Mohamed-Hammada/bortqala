package com.bemo.hr.product.support;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "support_tickets")
@Getter
public class SupportTicket {
    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "ticket_no", nullable = false, length = 40)
    private String ticketNo;
    @Column(nullable = false, length = 20)
    private String priority;
    @Column(nullable = false, length = 40)
    private String category;
    @Column(name = "module_code", nullable = false, length = 60)
    private String moduleCode;
    @Column(nullable = false, length = 200)
    private String screen;
    @Column(name = "business_impact", nullable = false, length = 1000)
    private String businessImpact;
    @Column(nullable = false, length = 4000)
    private String description;
    @Column(nullable = false, length = 30)
    private String status;
    @Column(name = "assigned_team", nullable = false, length = 80)
    private String assignedTeam;
    @Column(name = "sla_due_at", nullable = false)
    private Instant slaDueAt;
    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;
    @Column(name = "operation_id", nullable = false, length = 80)
    private String operationId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Column(name = "resolved_at")
    private Instant resolvedAt;
    @Version
    private long version;

    protected SupportTicket() {
    }

    public SupportTicket(String no, String priority, String category, String module, String screen, String impact, String description, String team, Instant due, String actor, String operation) {
        id = UUID.randomUUID().toString();
        ticketNo = no;
        this.priority = priority;
        this.category = category;
        moduleCode = module;
        this.screen = screen;
        businessImpact = impact;
        this.description = description;
        status = "NEW";
        assignedTeam = team;
        slaDueAt = due;
        createdBy = actor;
        operationId = operation;
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    public void transition(String next, String team) {
        status = next;
        assignedTeam = team;
        updatedAt = Instant.now();
        resolvedAt = Set.of("RESOLVED", "CLOSED").contains(next) ? updatedAt : null;
    }
}
