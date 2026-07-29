package com.bemo.hr.trade.procurement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "procurement_document_sequences")
public class ProcurementDocumentSequence {
    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "document_type", nullable = false, length = 30)
    private String documentType;

    @Column(name = "next_value", nullable = false)
    private long nextValue;

    protected ProcurementDocumentSequence() { }

    public ProcurementDocumentSequence(String documentType, long firstValue) {
        this.id = UUID.randomUUID().toString();
        this.documentType = documentType;
        this.nextValue = Math.max(1, firstValue);
    }

    public long takeNext() {
        return nextValue++;
    }
}
