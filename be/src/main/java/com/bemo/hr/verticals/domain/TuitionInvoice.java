package com.bemo.hr.verticals.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;

@Entity
@Table(name = "school_tuition_invoices")
@Getter
@Setter
@NoArgsConstructor
public class TuitionInvoice {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "tenant_id", length = 64, nullable = false)
    private String tenantId;

    @Column(name = "enrollment_id", length = 36, nullable = false)
    private String enrollmentId;

    @Column(name = "invoice_number", length = 64, nullable = false)
    private String invoiceNumber;

    @Column(name = "installment_name", length = 128, nullable = false)
    private String installmentName;

    @Column(name = "due_date", nullable = false)
    private long dueDate;

    @Column(name = "amount_due", precision = 18, scale = 2, nullable = false)
    private BigDecimal amountDue;

    @Column(name = "amount_paid", precision = 18, scale = 2, nullable = false)
    private BigDecimal amountPaid;

    @Column(name = "status", length = 32, nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;
}
