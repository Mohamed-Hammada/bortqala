package com.bemo.hr.medical.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "consent_forms")
@Getter
@Setter
@NoArgsConstructor
public class ConsentForm {

    public enum Relation {
        SELF,
        GUARDIAN,
        NEXT_OF_KIN
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

    @Column(name = "template_key", length = 64, nullable = false)
    private String templateKey;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "body_text", length = 4000, nullable = false)
    private String bodyText;

    @Column(name = "signed_by_name", length = 255, nullable = false)
    private String signedByName;

    @Enumerated(EnumType.STRING)
    @Column(name = "signed_by_relation", length = 32, nullable = false)
    private Relation signedByRelation = Relation.SELF;

    @Column(name = "signed_at", nullable = false)
    private long signedAt;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public ConsentForm(String patientId,
                       String visitId,
                       String templateKey,
                       String title,
                       String bodyText,
                       String signedByName,
                       Relation signedByRelation,
                       String ipAddress) {
        this.id = UUID.randomUUID().toString();
        this.patientId = patientId;
        this.visitId = visitId;
        this.templateKey = templateKey;
        this.title = title;
        this.bodyText = bodyText;
        this.signedByName = signedByName;
        this.signedByRelation = signedByRelation != null ? signedByRelation : Relation.SELF;
        this.signedAt = Instant.now().toEpochMilli();
        this.ipAddress = ipAddress;
        this.createdAt = this.signedAt;
        this.updatedAt = this.signedAt;
        this.version = 0L;
    }
}
