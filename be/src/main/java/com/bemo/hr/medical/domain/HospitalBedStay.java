package com.bemo.hr.medical.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hospital_bed_stays")
@Getter
@Setter
@NoArgsConstructor
public class HospitalBedStay {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "admission_id", length = 36, nullable = false)
    private String admissionId;

    @Column(name = "bed_id", length = 36, nullable = false)
    private String bedId;

    @Column(name = "started_at", nullable = false)
    private long startedAt;

    @Column(name = "ended_at")
    private Long endedAt;

    @Column(name = "transfer_reason", length = 255)
    private String transferReason;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public HospitalBedStay(String admissionId, String bedId, String transferReason) {
        this.id = UUID.randomUUID().toString();
        this.admissionId = admissionId;
        this.bedId = bedId;
        this.transferReason = transferReason;
        this.startedAt = Instant.now().toEpochMilli();
        this.createdAt = this.startedAt;
    }

    public void endStay() {
        this.endedAt = Instant.now().toEpochMilli();
    }
}
