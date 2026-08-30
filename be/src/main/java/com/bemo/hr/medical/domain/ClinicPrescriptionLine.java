package com.bemo.hr.medical.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "clinic_prescriptions")
@Getter
@Setter
@NoArgsConstructor
public class ClinicPrescriptionLine {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "visit_id", length = 36, nullable = false)
    private String visitId;

    @Column(name = "drug_name", length = 200, nullable = false)
    private String drugName;

    @Column(name = "dose", length = 100, nullable = false)
    private String dose;

    @Column(name = "frequency", length = 100, nullable = false)
    private String frequency;

    @Column(name = "duration", length = 100, nullable = false)
    private String duration;

    @Column(name = "instructions", length = 500)
    private String instructions;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public ClinicPrescriptionLine(String visitId, String drugName, String dose,
                                  String frequency, String duration, String instructions) {
        this.id = UUID.randomUUID().toString();
        this.visitId = visitId;
        this.drugName = drugName;
        this.dose = dose;
        this.frequency = frequency;
        this.duration = duration;
        this.instructions = instructions;
        this.createdAt = Instant.now();
    }
}
