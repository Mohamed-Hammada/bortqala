package com.bemo.hr.shared.i18n;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "translations")
public class TranslationEntry {
    @Id
    private String id;
    @Column(name = "translation_key", nullable = false, length = 150)
    private String translationKey;
    @Column(nullable = false, length = 10)
    private String locale;
    @Column(name = "text_value", nullable = false, columnDefinition = "TEXT")
    private String textValue;
    @Column(name = "app_id", length = 36)
    private String appId;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TranslationEntry() {
    }

    TranslationEntry(String translationKey, String locale, String textValue, String appId) {
        this.id = UUID.randomUUID().toString();
        this.translationKey = translationKey.strip();
        this.locale = locale;
        this.textValue = textValue;
        this.appId = appId;
    }

    public String getId() {
        return id;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public String getLocale() {
        return locale;
    }

    public String getTextValue() {
        return textValue;
    }

    public String getAppId() {
        return appId;
    }

    void updateTextValue(String textValue) {
        this.textValue = textValue;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
