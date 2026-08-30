package com.bemo.hr.compliance.eta.infrastructure;

import com.bemo.hr.compliance.eta.domain.EtaDocumentType;
import com.bemo.hr.compliance.eta.domain.EtaInvoiceSubmission;
import com.bemo.hr.compliance.eta.domain.EtaSubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface EtaInvoiceSubmissionRepository extends JpaRepository<EtaInvoiceSubmission, String> {

    Optional<EtaInvoiceSubmission> findByInvoiceId(String invoiceId);

    Optional<EtaInvoiceSubmission> findByEtaUuid(String etaUuid);

    List<EtaInvoiceSubmission> findByStatusOrderByDateTimeIssuedDesc(EtaSubmissionStatus status);

    List<EtaInvoiceSubmission> findByDocumentTypeOrderByDateTimeIssuedDesc(EtaDocumentType documentType);

    List<EtaInvoiceSubmission> findAllByOrderByDateTimeIssuedDesc();

    long countByStatus(EtaSubmissionStatus status);

    @Query("SELECT COALESCE(SUM(s.taxAmount), 0) FROM EtaInvoiceSubmission s WHERE s.status = 'VALID'")
    BigDecimal sumValidTaxAmount();
}
