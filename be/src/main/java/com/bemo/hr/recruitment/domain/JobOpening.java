package com.bemo.hr.recruitment.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "job_openings")
public class JobOpening {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "title_ar", nullable = false, length = 300)
    private String titleAr;

    @Column(name = "title_en", nullable = false, length = 300)
    private String titleEn;

    @Column(name = "department_id", length = 50)
    private String departmentId;

    @Column(name = "headcount", nullable = false)
    private int headcount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OpeningStatus status;

    @Column(name = "description", length = 4000)
    private String description;

    @Column(name = "published", nullable = false)
    private boolean published;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    protected JobOpening() {
    }

    public JobOpening(String titleAr, String titleEn, String departmentId, int headcount,
                      String description, boolean published) {
        this.id = UUID.randomUUID().toString();
        this.titleAr = titleAr;
        this.titleEn = titleEn;
        this.departmentId = departmentId;
        this.headcount = headcount;
        this.status = OpeningStatus.DRAFT;
        this.description = description;
        this.published = published;
    }

    public void update(String titleAr, String titleEn, String departmentId, int headcount,
                       String description, boolean published) {
        this.titleAr = titleAr;
        this.titleEn = titleEn;
        this.departmentId = departmentId;
        this.headcount = headcount;
        this.description = description;
        this.published = published;
    }

    public void publish() {
        if (this.status == OpeningStatus.CLOSED) {
            throw new com.bemo.hr.shared.domain.BusinessRuleException(
                    "Cannot publish a closed opening", "RECR_OPENING_CLOSED", org.springframework.http.HttpStatus.CONFLICT);
        }
        this.status = OpeningStatus.OPEN;
        this.published = true;
    }

    public void close() {
        this.status = OpeningStatus.CLOSED;
        this.published = false;
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
    public String getTitleAr() { return titleAr; }
    public String getTitleEn() { return titleEn; }
    public String getDepartmentId() { return departmentId; }
    public int getHeadcount() { return headcount; }
    public OpeningStatus getStatus() { return status; }
    public String getDescription() { return description; }
    public boolean isPublished() { return published; }
    public long getVersion() { return version; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
}
