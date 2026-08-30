package com.bemo.hr.medical.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "dental_treatment_plan_items")
@Getter
@Setter
@NoArgsConstructor
public class DentalTreatmentPlanItem {

    public enum Status {
        PLANNED,
        DONE,
        CANCELLED
    }

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "plan_id", length = 36, nullable = false)
    private String planId;

    @Column(name = "tooth_number", nullable = false)
    private Integer toothNumber;

    @Column(name = "procedure_text", length = 200, nullable = false)
    private String procedureText;

    @Column(name = "estimated_cost", precision = 15, scale = 2, nullable = false)
    private BigDecimal estimatedCost;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private Status status;

    @Column(name = "completed_at")
    private Long completedAt;

    @Column(name = "visit_id", length = 36)
    private String visitId;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public DentalTreatmentPlanItem(String planId, Integer toothNumber, String procedureText, BigDecimal estimatedCost) {
        this.id = UUID.randomUUID().toString();
        this.planId = planId;
        this.toothNumber = toothNumber;
        this.procedureText = procedureText;
        this.estimatedCost = estimatedCost != null ? estimatedCost : BigDecimal.ZERO;
        this.status = Status.PLANNED;
    }
}
