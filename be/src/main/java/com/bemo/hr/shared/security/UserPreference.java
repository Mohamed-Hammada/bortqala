package com.bemo.hr.shared.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_preferences")
@Getter
public class UserPreference {
    @Id private String id;
    @TenantId @Column(name = "app_id", nullable = false) private String appId;
    @Column(name = "user_id", nullable = false) private String userId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ThemePreference theme;
    @Enumerated(EnumType.STRING) @Column(name = "table_density", nullable = false, length = 20)
    private TableDensity tableDensity;
    @Column(nullable = false, length = 10) private String locale;
    @Enumerated(EnumType.STRING) @Column(name = "excel_table_style", nullable = false, length = 20)
    private ExcelTableStyle excelTableStyle;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected UserPreference() { }

    public UserPreference(String userId) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.theme = ThemePreference.SYSTEM;
        this.tableDensity = TableDensity.COMFORTABLE;
        this.locale = "ar-EG";
        this.excelTableStyle = ExcelTableStyle.GOLD;
    }

    public void update(ThemePreference theme, TableDensity tableDensity, String locale, ExcelTableStyle excelTableStyle) {
        this.theme = theme;
        this.tableDensity = tableDensity;
        this.locale = locale.equalsIgnoreCase("en-US") ? "en-US" : "ar-EG";
        this.excelTableStyle = excelTableStyle;
    }

    @PrePersist void prePersist() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void preUpdate() { updatedAt = Instant.now(); }

}
