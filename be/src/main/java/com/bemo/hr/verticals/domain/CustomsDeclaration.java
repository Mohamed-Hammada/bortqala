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
@Table(name = "customs_declaration_files")
@Getter
@Setter
@NoArgsConstructor
public class CustomsDeclaration {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "tenant_id", length = 64, nullable = false)
    private String tenantId;

    @Column(name = "file_number", length = 64, nullable = false)
    private String fileNumber;

    @Column(name = "importer_name", length = 255, nullable = false)
    private String importerName;

    @Column(name = "port_of_entry", length = 128, nullable = false)
    private String portOfEntry;

    @Column(name = "bill_of_lading_number", length = 128, nullable = false)
    private String billOfLadingNumber;

    @Column(name = "customs_certificate_number", length = 128)
    private String customsCertificateNumber;

    @Column(name = "duty_disbursement_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal dutyDisbursementAmount;

    @Column(name = "port_handling_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal portHandlingAmount;

    @Column(name = "clearance_service_fee", precision = 18, scale = 2, nullable = false)
    private BigDecimal clearanceServiceFee;

    @Column(name = "total_invoice_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal totalInvoiceAmount;

    @Column(name = "status", length = 32, nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;
}
