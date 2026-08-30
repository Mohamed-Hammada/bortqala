package com.bemo.hr.medical.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hospital_wards")
@Getter
@Setter
@NoArgsConstructor
public class HospitalWard {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "code", length = 30, nullable = false)
    private String code;

    @Column(name = "name", length = 160, nullable = false)
    private String name;

    @Column(name = "department_id", length = 36)
    private String departmentId;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public HospitalWard(String code, String name, String departmentId) {
        this.id = UUID.randomUUID().toString();
        this.code = code;
        this.name = name;
        this.departmentId = departmentId;
        this.active = true;
        this.createdAt = Instant.now().toEpochMilli();
        this.updatedAt = this.createdAt;
    }
}
