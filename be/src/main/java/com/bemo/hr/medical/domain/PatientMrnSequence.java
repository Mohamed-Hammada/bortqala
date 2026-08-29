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
@Table(name = "patient_mrn_sequences")
@Getter
@Setter
@NoArgsConstructor
public class PatientMrnSequence {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "current_sequence", nullable = false)
    private Long currentSequence;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public PatientMrnSequence(String appId, Long currentSequence) {
        this.id = UUID.randomUUID().toString();
        this.appId = appId;
        this.currentSequence = currentSequence;
        this.updatedAt = Instant.now();
    }

    public long next() {
        this.currentSequence++;
        this.updatedAt = Instant.now();
        return this.currentSequence;
    }
}
