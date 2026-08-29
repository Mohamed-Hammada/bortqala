package com.bemo.hr.medical.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pharmacy_items")
@Getter
@Setter
@NoArgsConstructor
public class PharmacyItem {

    public enum DosageForm {
        TABLET,
        SYRUP,
        INJECTION,
        CAPSULE,
        OINTMENT,
        DROPS,
        INHALER
    }

    public enum ControlSchedule {
        SCHEDULE_I,
        SCHEDULE_II,
        SCHEDULE_III,
        SCHEDULE_IV,
        SCHEDULE_V
    }

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 64, nullable = false)
    private String appId;

    @Column(name = "item_id", length = 64, nullable = false)
    private String itemId;

    @Column(name = "trade_name", length = 255, nullable = false)
    private String tradeName;

    @Column(name = "generic_name", length = 255)
    private String genericName;

    @Enumerated(EnumType.STRING)
    @Column(name = "dosage_form", length = 64, nullable = false)
    private DosageForm dosageForm = DosageForm.TABLET;

    @Column(name = "strength_text", length = 128)
    private String strengthText;

    @Column(name = "is_controlled", nullable = false)
    private boolean controlled = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "control_schedule", length = 32)
    private ControlSchedule controlSchedule;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public PharmacyItem(String itemId,
                        String tradeName,
                        String genericName,
                        DosageForm dosageForm,
                        String strengthText,
                        boolean controlled,
                        ControlSchedule controlSchedule) {
        this.id = UUID.randomUUID().toString();
        this.itemId = itemId;
        this.tradeName = tradeName;
        this.genericName = genericName;
        this.dosageForm = dosageForm != null ? dosageForm : DosageForm.TABLET;
        this.strengthText = strengthText;
        this.controlled = controlled;
        this.controlSchedule = controlSchedule;
        this.createdAt = Instant.now().toEpochMilli();
        this.updatedAt = this.createdAt;
        this.version = 0L;
    }
}
