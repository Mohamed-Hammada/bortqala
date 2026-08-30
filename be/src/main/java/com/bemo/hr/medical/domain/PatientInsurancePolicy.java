package com.bemo.hr.medical.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "patient_insurance_policies")
@Getter
@Setter
@NoArgsConstructor
public class PatientInsurancePolicy {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "patient_id", length = 36, nullable = false)
    private String patientId;

    @Column(name = "plan_id", length = 36, nullable = false)
    private String planId;

    @Column(name = "member_number", length = 60, nullable = false)
    private String memberNumber;

    @Column(name = "valid_from", length = 10, nullable = false)
    private String validFrom;

    @Column(name = "valid_to", length = 10, nullable = false)
    private String validTo;

    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary = true;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public PatientInsurancePolicy(String patientId, String planId, String memberNumber,
                                  String validFrom, String validTo, boolean isPrimary) {
        this.id = UUID.randomUUID().toString();
        this.patientId = patientId;
        this.planId = planId;
        this.memberNumber = memberNumber;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.isPrimary = isPrimary;
        this.createdAt = Instant.now().toEpochMilli();
        this.updatedAt = this.createdAt;
    }
}
