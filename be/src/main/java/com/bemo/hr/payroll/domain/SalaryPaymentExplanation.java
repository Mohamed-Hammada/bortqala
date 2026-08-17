package com.bemo.hr.payroll.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "salary_payment_explanations")
@Getter
public class SalaryPaymentExplanation {
    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "salary_payment_id", nullable = false, length = 36)
    private String salaryPaymentId;
    @Column(name = "component_type", nullable = false, length = 50)
    private String componentType;
    @Column(nullable = false, length = 500)
    private String formula;
    @Column(name = "input_values_json", length = 1000)
    private String inputValuesJson;
    @Column(name = "calculated_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal calculatedAmount;
    @Column(name = "explanation_text_ar", length = 1000)
    private String explanationTextAr;
    @Column(name = "explanation_text_en", length = 1000)
    private String explanationTextEn;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected SalaryPaymentExplanation() {
    }

    public SalaryPaymentExplanation(String salaryPaymentId, String componentType, String formula,
                                    String inputValuesJson, BigDecimal calculatedAmount,
                                    String explanationTextAr, String explanationTextEn) {
        this.id = UUID.randomUUID().toString();
        this.salaryPaymentId = salaryPaymentId;
        this.componentType = componentType.strip().toUpperCase();
        this.formula = formula.strip();
        this.inputValuesJson = inputValuesJson;
        this.calculatedAmount = calculatedAmount != null ? calculatedAmount : BigDecimal.ZERO;
        this.explanationTextAr = explanationTextAr;
        this.explanationTextEn = explanationTextEn;
        this.createdAt = Instant.now();
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
