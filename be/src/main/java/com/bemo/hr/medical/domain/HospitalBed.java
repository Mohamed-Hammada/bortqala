package com.bemo.hr.medical.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hospital_beds")
@Getter
@Setter
@NoArgsConstructor
public class HospitalBed {

    public enum Status {
        FREE, OCCUPIED, MAINTENANCE, ISOLATION
    }

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "room_id", length = 36, nullable = false)
    private String roomId;

    @Column(name = "bed_number", length = 30, nullable = false)
    private String bedNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private Status status = Status.FREE;

    @Column(name = "current_admission_id", length = 36)
    private String currentAdmissionId;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public HospitalBed(String roomId, String bedNumber, Status status) {
        this.id = UUID.randomUUID().toString();
        this.roomId = roomId;
        this.bedNumber = bedNumber;
        this.status = status != null ? status : Status.FREE;
        this.active = true;
        this.createdAt = Instant.now().toEpochMilli();
        this.updatedAt = this.createdAt;
    }

    public void occupy(String admissionId) {
        this.status = Status.OCCUPIED;
        this.currentAdmissionId = admissionId;
        this.updatedAt = Instant.now().toEpochMilli();
    }

    public void free() {
        this.status = Status.FREE;
        this.currentAdmissionId = null;
        this.updatedAt = Instant.now().toEpochMilli();
    }
}
