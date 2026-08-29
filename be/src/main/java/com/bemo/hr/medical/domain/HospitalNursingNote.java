package com.bemo.hr.medical.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hospital_nursing_notes")
@Getter
@Setter
@NoArgsConstructor
public class HospitalNursingNote {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "admission_id", length = 36, nullable = false)
    private String admissionId;

    @Column(name = "recorded_at", nullable = false)
    private long recordedAt;

    @Column(name = "nurse_name", length = 160, nullable = false)
    private String nurseName;

    @Column(name = "note_text", length = 2000, nullable = false)
    private String noteText;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public HospitalNursingNote(String admissionId, String nurseName, String noteText) {
        this.id = UUID.randomUUID().toString();
        this.admissionId = admissionId;
        this.nurseName = nurseName;
        this.noteText = noteText;
        this.recordedAt = Instant.now().toEpochMilli();
    }
}
