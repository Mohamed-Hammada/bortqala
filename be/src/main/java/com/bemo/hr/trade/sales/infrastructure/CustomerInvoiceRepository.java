package com.bemo.hr.trade.sales.infrastructure;

import com.bemo.hr.trade.sales.domain.CustomerInvoice;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
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

    /**
     * 2026-09-07 remediation (Performance Review P-1): the Executive Analytics AR-aging path used
     * to call {@code findAll()} and filter {@code outstandingAmount > 0} in a Java stream after
     * hydrating every invoice the tenant has ever issued. This pushes the filter into SQL — for a
     * tenant with 50k invoices where only a few hundred are open, this is the difference between
     * hydrating 50k rows and hydrating a few hundred. Supported by {@code idx_customer_invoices_app_outstanding}.
     */
    List<CustomerInvoice> findByOutstandingAmountGreaterThan(BigDecimal amount);

    /** Same rationale as {@link #findByOutstandingAmountGreaterThan}, scoped to a calendar-date range for period-scoped revenue sums. */
    List<CustomerInvoice> findByInvoiceDateBetween(java.time.LocalDate start, java.time.LocalDate end);

    List<CustomerInvoice> findTop10ByInvoiceNumberContainingIgnoreCaseOrderByInvoiceDateDesc(String invoiceNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM CustomerInvoice i WHERE i.id IN :ids")
    List<CustomerInvoice> findAllByIdForUpdate(@Param("ids") Collection<String> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM CustomerInvoice i WHERE i.id = :id")
    java.util.Optional<CustomerInvoice> findByIdForUpdate(@Param("id") String id);

    @Query("SELECT COALESCE(SUM(i.outstandingAmount), 0) FROM CustomerInvoice i WHERE i.customerId = :customerId AND i.status <> com.bemo.hr.trade.sales.domain.CustomerInvoice.Status.DRAFT")
    BigDecimal outstanding(@Param("customerId") String customerId);

    /**
     * 2026-09-07 remediation (Performance Review P-1): Executive Analytics' "Top Customers" used to
     * load EVERY invoice the tenant has ever issued into memory just to group/sum/sort/limit them
     * in a Java stream. This does the grouping, summing, and ordering in SQL and only ever returns
     * as many rows as requested (via {@code pageable}, e.g. {@code PageRequest.of(0, 5)}).
     * Supported by {@code idx_customer_invoices_app_customer}.
     */
    @Query("SELECT i.customerId AS customerId, COALESCE(SUM(i.amount), 0) AS invoiced, "
            + "COALESCE(SUM(i.outstandingAmount), 0) AS outstanding, COUNT(i) AS invoiceCount "
            + "FROM CustomerInvoice i WHERE i.customerId IS NOT NULL "
            + "GROUP BY i.customerId ORDER BY SUM(i.amount) DESC")
    List<CustomerRevenueSummary> topCustomersByInvoicedAmount(Pageable pageable);

    interface CustomerRevenueSummary {
        String getCustomerId();
        BigDecimal getInvoiced();
        BigDecimal getOutstanding();
        long getInvoiceCount();
    }
}
