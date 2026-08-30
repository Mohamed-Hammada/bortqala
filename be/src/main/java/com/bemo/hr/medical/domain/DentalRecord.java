package com.bemo.hr.medical.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "dental_records")
@Getter
@Setter
@NoArgsConstructor
public class DentalRecord {

    public enum Condition {
        HEALTHY,
        CARIES,
        FILLED,
        CROWN,
        MISSING,
        IMPLANT,
        ROOT_CANAL,
        EXTRACTION_PLANNED
    }

    public enum Surface {
        OCCLUSAL,
        MESIAL,
        DISTAL,
        BUCCAL,
        LINGUAL
    }

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "patient_id", length = 36, nullable = false)
    private String patientId;

    @Column(name = "visit_id", length = 36)
    private String visitId;

    @Column(name = "tooth_number", nullable = false)
    private Integer toothNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition", length = 30, nullable = false)
    private Condition condition;

    @Enumerated(EnumType.STRING)
    @Column(name = "surface", length = 20)
    private Surface surface;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "noted_on", nullable = false)
    private Long notedOn;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public DentalRecord(String patientId, String visitId, Integer toothNumber,
                        Condition condition, Surface surface, String notes, Long notedOn) {
        this.id = UUID.randomUUID().toString();
        this.patientId = patientId;
        this.visitId = visitId;
        this.toothNumber = toothNumber;
        this.condition = condition;
        this.surface = surface;
        this.notes = notes;
        this.notedOn = notedOn != null ? notedOn : System.currentTimeMillis();
    }
}
