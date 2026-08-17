package com.bemo.hr.employee.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "employee_code_sequences")
public class EmployeeCodeSequence {
    @Id
    @Column(name = "category_id", nullable = false)
    private String categoryId;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "next_value", nullable = false)
    private long nextValue;

    protected EmployeeCodeSequence() {
    }

    public EmployeeCodeSequence(String categoryId) {
        this.categoryId = categoryId;
        this.nextValue = 1;
    }

    public long takeNext() {
        return nextValue++;
    }
}
