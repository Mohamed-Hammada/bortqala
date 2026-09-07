package com.bemo.hr.trade.procurement.infrastructure;

import com.bemo.hr.trade.procurement.domain.SupplierInvoice;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierInvoiceRepository extends JpaRepository<SupplierInvoice, String> {
    List<SupplierInvoice> findAllByOrderByInvoiceDateDesc();

    /**
     * 2026-09-07 remediation (Performance Review P-1): Executive Analytics' AP-aging path used to
     * call {@code findAll()} and filter non-PAID invoices in a Java stream. Pushes the filter into
     * SQL. {@code status} is always an uppercase {@code Status.name()} value, so a plain string
     * comparison is safe. Supported by {@code idx_supplier_invoices_app_status}.
     */
    List<SupplierInvoice> findByStatusNot(String status);

    List<SupplierInvoice> findBySupplierId(String supplierId);

    List<SupplierInvoice> findByPurchaseOrderId(String purchaseOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
                select invoice
                from SupplierInvoice invoice
                where invoice.id = :id
            """)
    Optional<SupplierInvoice> findByIdForPayment(@Param("id") String id);
}
