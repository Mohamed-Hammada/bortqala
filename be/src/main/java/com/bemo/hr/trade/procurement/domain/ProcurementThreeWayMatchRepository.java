package com.bemo.hr.trade.procurement.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProcurementThreeWayMatchRepository extends JpaRepository<ProcurementThreeWayMatch, String> {
    Optional<ProcurementThreeWayMatch> findBySupplierInvoiceId(String supplierInvoiceId);
}
