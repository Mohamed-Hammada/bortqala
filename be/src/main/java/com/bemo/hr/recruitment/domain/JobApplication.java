package com.bemo.hr.recruitment.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "job_applications")
public class JobApplication {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "opening_id", nullable = false)
    private String openingId;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "email", length = 200)
    private String email;

    @Column(name = "source", length = 50)
    private String source;

    @Column(name = "cv_attachment_id", length = 100)
    private String cvAttachmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 20)
    private ApplicationStage stage;

    @Column(name = "rating")
    private Integer rating;

    @Column(name = "notes", length = 4000)
    private String notes;

    @Column(name = "converted_employee_id", length = 100)
    private String convertedEmployeeId;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    protected JobApplication() {
    }

    public JobApplication(String openingId, String fullName, String phone, String email,
                          String source, String cvAttachmentId) {
        this.id = UUID.randomUUID().toString();
        this.openingId = openingId;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.source = source;
        this.cvAttachmentId = cvAttachmentId;
        this.stage = ApplicationStage.NEW;
    }

    public void moveToStage(ApplicationStage newStage) {
        this.stage = newStage;
    }

    public void markHired(String employeeId) {
        this.stage = ApplicationStage.HIRED;
        this.convertedEmployeeId = employeeId;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @PrePersist
    void prePersist() {
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getOpeningId() { return openingId; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getSource() { return source; }
    public String getCvAttachmentId() { return cvAttachmentId; }
    public ApplicationStage getStage() { return stage; }
    public Integer getRating() { return rating; }
    public String getNotes() { return notes; }
    public String getConvertedEmployeeId() { return convertedEmployeeId; }
    public long getVersion() { return version; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
}
