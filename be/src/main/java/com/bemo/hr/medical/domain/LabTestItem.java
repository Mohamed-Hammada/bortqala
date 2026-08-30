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
@Table(name = "lab_tests_catalog")
@Getter
@Setter
@NoArgsConstructor
public class LabTestItem {

    public enum Category {
        LAB,
        IMAGING
    }

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 64, nullable = false)
    private String appId;

    @Column(name = "code", length = 64, nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 32, nullable = false)
    private Category category = Category.LAB;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "sample_type", length = 64)
    private String sampleType;

    @Column(name = "normal_range_text", length = 255)
    private String normalRangeText;

    @Column(name = "price", precision = 12, scale = 2, nullable = false)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public LabTestItem(String code,
                       Category category,
                       String name,
                       String sampleType,
                       String normalRangeText,
                       BigDecimal price) {
        this.id = UUID.randomUUID().toString();
        this.code = code;
        this.category = category != null ? category : Category.LAB;
        this.name = name;
        this.sampleType = sampleType;
        this.normalRangeText = normalRangeText;
        this.price = price != null ? price : BigDecimal.ZERO;
        this.createdAt = Instant.now().toEpochMilli();
        this.updatedAt = this.createdAt;
        this.version = 0L;
    }
}
