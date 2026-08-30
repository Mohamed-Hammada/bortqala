package com.bemo.hr.trade.procurement.infrastructure;

import com.bemo.hr.trade.procurement.domain.SupplierPaymentPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierPaymentPlanRepository extends JpaRepository<SupplierPaymentPlan, String> {

    boolean existsByInvoiceId(String invoiceId);

    List<SupplierPaymentPlan> findByInvoiceIdOrderByInstallmentNoAsc(String invoiceId);
}
