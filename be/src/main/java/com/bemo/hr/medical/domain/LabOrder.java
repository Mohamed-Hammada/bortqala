package com.bemo.hr.medical.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lab_orders")
@Getter
@Setter
@NoArgsConstructor
public class LabOrder {

    public enum Status {
        ORDERED,
        COLLECTED,
        SENT_OUT,
        RESULTED,
        VALIDATED,
        CANCELLED
    }

    public enum ResultFlag {
        NORMAL,
        LOW,
        HIGH,
        CRITICAL
    }

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 64, nullable = false)
    private String appId;

    @Column(name = "patient_id", length = 64, nullable = false)
    private String patientId;

    @Column(name = "visit_id", length = 64)
    private String visitId;

    @Column(name = "doctor_employee_id", length = 64, nullable = false)
    private String doctorEmployeeId;

    @Column(name = "test_id", length = 64, nullable = false)
    private String testId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 32, nullable = false)
    private LabTestItem.Category category = LabTestItem.Category.LAB;

    @Column(name = "test_code", length = 64, nullable = false)
    private String testCode;

    @Column(name = "test_name", length = 255, nullable = false)
    private String testName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private Status status = Status.ORDERED;

    @Column(name = "ordered_at", nullable = false)
    private long orderedAt;

    @Column(name = "collected_at")
    private Long collectedAt;

    @Column(name = "sent_out_at")
    private Long sentOutAt;

    @Column(name = "resulted_at")
    private Long resultedAt;

    @Column(name = "validated_at")
    private Long validatedAt;

    @Column(name = "result_value_text", length = 500)
    private String resultValueText;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_flag", length = 32)
    private ResultFlag resultFlag = ResultFlag.NORMAL;

    @Column(name = "result_notes", length = 1000)
    private String resultNotes;

    @Column(name = "external_lab_party_id", length = 64)
    private String externalLabPartyId;

    @Column(name = "external_lab_name", length = 255)
    private String externalLabName;

    @Column(name = "attachment_id", length = 64)
    private String attachmentId;

    @Column(name = "attachment_filename", length = 255)
    private String attachmentFilename;

    @Column(name = "is_critical_acknowledged", nullable = false)
    private boolean criticalAcknowledged = false;

    @Column(name = "critical_acknowledged_at")
    private Long criticalAcknowledgedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public LabOrder(String patientId,
                    String visitId,
                    String doctorEmployeeId,
                    String testId,
                    LabTestItem.Category category,
                    String testCode,
                    String testName,
                    String externalLabPartyId,
                    String externalLabName) {
        this.id = UUID.randomUUID().toString();
        this.patientId = patientId;
        this.visitId = visitId;
        this.doctorEmployeeId = doctorEmployeeId;
        this.testId = testId;
        this.category = category != null ? category : LabTestItem.Category.LAB;
        this.testCode = testCode;
        this.testName = testName;
        this.externalLabPartyId = externalLabPartyId;
        this.externalLabName = externalLabName;
        this.status = Status.ORDERED;
        this.orderedAt = Instant.now().toEpochMilli();
        this.version = 0L;
    }

    public void markCollected() {
        this.status = Status.COLLECTED;
        this.collectedAt = Instant.now().toEpochMilli();
    }

    public void markSentOut(String labPartyId, String labName) {
        this.status = Status.SENT_OUT;
        this.sentOutAt = Instant.now().toEpochMilli();
        if (labPartyId != null) this.externalLabPartyId = labPartyId;
        if (labName != null) this.externalLabName = labName;
    }

    public void enterResult(String resultValueText, ResultFlag flag, String notes, String attachId, String attachFilename) {
        this.status = Status.RESULTED;
        this.resultedAt = Instant.now().toEpochMilli();
        this.resultValueText = resultValueText;
        this.resultFlag = flag != null ? flag : ResultFlag.NORMAL;
        this.resultNotes = notes;
        this.attachmentId = attachId;
        this.attachmentFilename = attachFilename;
    }

    public void validateResult() {
        this.status = Status.VALIDATED;
        this.validatedAt = Instant.now().toEpochMilli();
    }

    public void cancel() {
        this.status = Status.CANCELLED;
    }

    public void acknowledgeCritical() {
        this.criticalAcknowledged = true;
        this.criticalAcknowledgedAt = Instant.now().toEpochMilli();
    }
}
