package com.bemo.hr.medical.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "exam_templates")
@Getter
@Setter
@NoArgsConstructor
public class ExamTemplate {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "specialty", length = 60, nullable = false)
    private String specialty;

    @Column(name = "name", length = 160, nullable = false)
    private String name;

    @Column(name = "schema_json", length = 4000, nullable = false)
    private String schemaJson;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public ExamTemplate(String specialty, String name, String schemaJson) {
        this.id = UUID.randomUUID().toString();
        this.specialty = specialty;
        this.name = name;
        this.schemaJson = schemaJson;
        this.active = true;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
    }
}
