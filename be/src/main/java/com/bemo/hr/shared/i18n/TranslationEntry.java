package com.bemo.hr.shared.i18n;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "translations")
public class TranslationEntry {
    @Id private String id;
    @Column(name = "translation_key", nullable = false, length = 150) private String translationKey;
    @Column(nullable = false, length = 10) private String locale;
    @Column(name = "text_value", nullable = false, columnDefinition = "TEXT") private String textValue;

    protected TranslationEntry() { }

    public String getTranslationKey() { return translationKey; }
    public String getTextValue() { return textValue; }
}
