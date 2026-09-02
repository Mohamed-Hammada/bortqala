package com.bemo.hr.trade.sales.infrastructure;

import com.bemo.hr.trade.sales.domain.CustomerInvoice;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

@Repository
public interface CustomerInvoiceRepository extends JpaRepository<CustomerInvoice, String> {

    List<CustomerInvoice> findBySalesOrderId(String salesOrderId);

    boolean existsByInvoiceNumberIgnoreCase(String invoiceNumber);

List<CustomerInvoice> findAllByOrderByInvoiceDateDescCreatedAtDesc();

    List<CustomerInvoice> findTop10ByInvoiceNumberContainingIgnoreCaseOrderByInvoiceDateDesc(String invoiceNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM CustomerInvoice i WHERE i.id IN :ids")
    List<CustomerInvoice> findAllByIdForUpdate(@Param("ids") Collection<String> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM CustomerInvoice i WHERE i.id = :id")
    java.util.Optional<CustomerInvoice> findByIdForUpdate(@Param("id") String id);

    @Query("SELECT COALESCE(SUM(i.outstandingAmount), 0) FROM CustomerInvoice i WHERE i.customerId = :customerId AND i.status <> com.bemo.hr.trade.sales.domain.CustomerInvoice.Status.DRAFT")
    BigDecimal outstanding(@Param("customerId") String customerId);
}
