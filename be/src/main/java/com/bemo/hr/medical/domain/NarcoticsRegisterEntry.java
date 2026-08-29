package com.bemo.hr.medical.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "narcotics_register")
@Getter
@Setter
@NoArgsConstructor
public class NarcoticsRegisterEntry {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 64, nullable = false)
    private String appId;

    @Column(name = "dispense_record_id", length = 64, nullable = false)
    private String dispenseRecordId;

    @Column(name = "pharmacy_item_id", length = 64, nullable = false)
    private String pharmacyItemId;

    @Column(name = "trade_name", length = 255, nullable = false)
    private String tradeName;

    @Column(name = "patient_mrn", length = 64, nullable = false)
    private String patientMrn;

    @Column(name = "patient_name", length = 255, nullable = false)
    private String patientName;

    @Column(name = "prescriber_doctor_name", length = 255, nullable = false)
    private String prescriberDoctorName;

    @Column(name = "dispenser_user_name", length = 255, nullable = false)
    private String dispenserUserName;

    @Column(name = "second_signer_name", length = 255, nullable = false)
    private String secondSignerName;

    @Column(name = "batch_number", length = 64)
    private String batchNumber;

    @Column(name = "quantity", precision = 12, scale = 3, nullable = false)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "signed_at", nullable = false)
    private long signedAt;

    public NarcoticsRegisterEntry(String dispenseRecordId,
                                  String pharmacyItemId,
                                  String tradeName,
                                  String patientMrn,
                                  String patientName,
                                  String prescriberDoctorName,
                                  String dispenserUserName,
                                  String secondSignerName,
                                  String batchNumber,
                                  BigDecimal quantity,
                                  String reason) {
        this.id = UUID.randomUUID().toString();
        this.dispenseRecordId = dispenseRecordId;
        this.pharmacyItemId = pharmacyItemId;
        this.tradeName = tradeName;
        this.patientMrn = patientMrn;
        this.patientName = patientName;
        this.prescriberDoctorName = prescriberDoctorName;
        this.dispenserUserName = dispenserUserName;
        this.secondSignerName = secondSignerName;
        this.batchNumber = batchNumber;
        this.quantity = quantity != null ? quantity : BigDecimal.ZERO;
        this.reason = reason;
        this.signedAt = Instant.now().toEpochMilli();
    }
}
