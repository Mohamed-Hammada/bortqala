package com.bemo.hr.trade.procurement.infrastructure;

import com.bemo.hr.trade.procurement.domain.SupplierInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierInvoiceRepository extends JpaRepository<SupplierInvoice, String> {
    List<SupplierInvoice> findAllByOrderByInvoiceDateDesc();
    List<SupplierInvoice> findBySupplierId(String supplierId);
    List<SupplierInvoice> findByPurchaseOrderId(String purchaseOrderId);
}
