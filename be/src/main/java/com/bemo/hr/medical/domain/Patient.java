package com.bemo.hr.medical.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "medical_patients")
@Getter
@Setter
@NoArgsConstructor
public class Patient {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "mrn", length = 30, nullable = false)
    private String mrn;

    @Column(name = "national_id", length = 14)
    private String nationalId;

    @Column(name = "full_name", length = 160, nullable = false)
    private String fullName;

    @Column(name = "phone", length = 30, nullable = false)
    private String phone;

    @Column(name = "gender", length = 10, nullable = false)
    private String gender;

    @Column(name = "birth_date", length = 10)
    private String birthDate;

    @Column(name = "blood_group", length = 10)
    private String bloodGroup;

    @Column(name = "allergies_text", length = 1000)
    private String allergiesText;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "emergency_contact_name", length = 160)
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", length = 30)
    private String emergencyContactPhone;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public Patient(String mrn, String nationalId, String fullName, String phone,
                   String gender, String birthDate, String bloodGroup,
                   String allergiesText, String notes,
                   String emergencyContactName, String emergencyContactPhone) {
        this.id = UUID.randomUUID().toString();
        this.mrn = mrn;
        this.nationalId = nationalId;
        this.fullName = fullName;
        this.phone = phone;
        this.gender = gender;
        this.birthDate = birthDate;
        this.bloodGroup = bloodGroup;
        this.allergiesText = allergiesText;
        this.notes = notes;
        this.emergencyContactName = emergencyContactName;
        this.emergencyContactPhone = emergencyContactPhone;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        this.version = 0L;
    }
}
