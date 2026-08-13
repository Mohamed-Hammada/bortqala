package com.bemo.hr.trade.procurement.infrastructure;

import com.bemo.hr.trade.procurement.domain.SupplierPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface SupplierPaymentRepository extends JpaRepository<SupplierPayment, String> {
    List<SupplierPayment> findAllByOrderByPaymentDateDesc();
    List<SupplierPayment> findBySupplierInvoiceId(String supplierInvoiceId);
    Optional<SupplierPayment> findByOperationId(String operationId);
    boolean existsByPaymentNumberIgnoreCase(String paymentNumber);

    @Query("select coalesce(sum(payment.amount), 0) from SupplierPayment payment where payment.supplierInvoiceId = :invoiceId and payment.status = 'POSTED'")
    BigDecimal sumPostedAmountBySupplierInvoiceId(@Param("invoiceId") String invoiceId);
}
