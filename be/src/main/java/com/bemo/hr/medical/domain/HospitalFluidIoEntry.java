package com.bemo.hr.medical.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hospital_fluid_io_entries")
@Getter
@Setter
@NoArgsConstructor
public class HospitalFluidIoEntry {

    public enum Type {
        INTAKE, OUTPUT
    }

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "admission_id", length = 36, nullable = false)
    private String admissionId;

    @Column(name = "entry_time", nullable = false)
    private long entryTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 10, nullable = false)
    private Type type;

    @Column(name = "route_or_fluid", length = 60, nullable = false)
    private String routeOrFluid;

    @Column(name = "amount_ml", nullable = false)
    private int amountMl;

    @Column(name = "recorded_by", length = 160)
    private String recordedBy;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public HospitalFluidIoEntry(String admissionId, Type type, String routeOrFluid, int amountMl, String recordedBy) {
        this.id = UUID.randomUUID().toString();
        this.admissionId = admissionId;
        this.type = type;
        this.routeOrFluid = routeOrFluid;
        this.amountMl = amountMl;
        this.recordedBy = recordedBy;
        this.entryTime = Instant.now().toEpochMilli();
    }
}
