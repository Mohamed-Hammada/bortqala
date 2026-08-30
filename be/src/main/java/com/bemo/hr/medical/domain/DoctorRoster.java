package com.bemo.hr.medical.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "doctor_rosters")
@Getter
@Setter
@NoArgsConstructor
public class DoctorRoster {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 64, nullable = false)
    private String appId;

    @Column(name = "doctor_employee_id", length = 64, nullable = false)
    private String doctorEmployeeId;

    @Column(name = "weekday", nullable = false)
    private int weekday; // 0=Sunday, 1=Monday, ..., 6=Saturday

    @Column(name = "start_time", length = 8, nullable = false)
    private String startTime; // "09:00"

    @Column(name = "end_time", length = 8, nullable = false)
    private String endTime; // "17:00"

    @Column(name = "slot_minutes", nullable = false)
    private int slotMinutes = 20;

    @Column(name = "max_patients_per_slot", nullable = false)
    private int maxPatientsPerSlot = 1;

    @Column(name = "valid_from", length = 16)
    private String validFrom;

    @Column(name = "valid_to", length = 16)
    private String validTo;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public DoctorRoster(String doctorEmployeeId, int weekday, String startTime, String endTime, int slotMinutes, int maxPatientsPerSlot, String validFrom, String validTo) {
        this.id = UUID.randomUUID().toString();
        this.doctorEmployeeId = doctorEmployeeId;
        this.weekday = weekday;
        this.startTime = startTime;
        this.endTime = endTime;
        this.slotMinutes = slotMinutes > 0 ? slotMinutes : 20;
        this.maxPatientsPerSlot = maxPatientsPerSlot > 0 ? maxPatientsPerSlot : 1;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.active = true;
        this.createdAt = Instant.now().toEpochMilli();
        this.updatedAt = this.createdAt;
        this.version = 0L;
    }
}
