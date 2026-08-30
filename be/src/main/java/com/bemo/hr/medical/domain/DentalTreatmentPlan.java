package com.bemo.hr.medical.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "dental_treatment_plans")
@Getter
@Setter
@NoArgsConstructor
public class DentalTreatmentPlan {

    public enum Status {
        DRAFT,
        ACTIVE,
        COMPLETED,
        CANCELLED
    }

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "patient_id", length = 36, nullable = false)
    private String patientId;

    @Column(name = "title", length = 160, nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private Status status;

    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @OneToMany(mappedBy = "planId", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DentalTreatmentPlanItem> items = new ArrayList<>();

    public DentalTreatmentPlan(String patientId, String title) {
        this.id = UUID.randomUUID().toString();
        this.patientId = patientId;
        this.title = title;
        this.status = Status.ACTIVE;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
    }
}
