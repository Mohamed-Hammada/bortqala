package com.bemo.hr.medical.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "visit_vitals")
@Getter
@Setter
@NoArgsConstructor
public class VisitVitals {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 64, nullable = false)
    private String appId;

    @Column(name = "visit_id", length = 64, nullable = false)
    private String visitId;

    @Column(name = "patient_id", length = 64, nullable = false)
    private String patientId;

    @Column(name = "systolic_bp")
    private Integer systolicBp;

    @Column(name = "diastolic_bp")
    private Integer diastolicBp;

    @Column(name = "pulse")
    private Integer pulse;

    @Column(name = "temp_c", precision = 4, scale = 1)
    private BigDecimal tempC;

    @Column(name = "spo2")
    private Integer spo2;

    @Column(name = "weight_kg", precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "height_cm", precision = 5, scale = 1)
    private BigDecimal heightCm;

    @Column(name = "bmi", precision = 4, scale = 1)
    private BigDecimal bmi;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "recorded_at", nullable = false)
    private long recordedAt;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public VisitVitals(String visitId,
                       String patientId,
                       Integer systolicBp,
                       Integer diastolicBp,
                       Integer pulse,
                       BigDecimal tempC,
                       Integer spo2,
                       BigDecimal weightKg,
                       BigDecimal heightCm,
                       String notes) {
        this.id = UUID.randomUUID().toString();
        this.visitId = visitId;
        this.patientId = patientId;
        this.systolicBp = systolicBp;
        this.diastolicBp = diastolicBp;
        this.pulse = pulse;
        this.tempC = tempC;
        this.spo2 = spo2;
        this.weightKg = weightKg;
        this.heightCm = heightCm;
        this.notes = notes;
        this.recordedAt = Instant.now().toEpochMilli();
        this.createdAt = this.recordedAt;
        this.updatedAt = this.recordedAt;
        this.version = 0L;
        this.bmi = calculateBmi(weightKg, heightCm);
    }

    public static BigDecimal calculateBmi(BigDecimal weightKg, BigDecimal heightCm) {
        if (weightKg == null || heightCm == null || heightCm.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        BigDecimal heightMeters = heightCm.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        BigDecimal heightSquared = heightMeters.multiply(heightMeters);
        if (heightSquared.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return weightKg.divide(heightSquared, 1, RoundingMode.HALF_UP);
    }
}
