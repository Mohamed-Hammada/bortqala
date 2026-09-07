package com.bemo.hr.trade.sales.infrastructure;

import com.bemo.hr.trade.sales.domain.QuotationStatus;
import com.bemo.hr.trade.sales.domain.SalesQuotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SalesQuotationRepository extends JpaRepository<SalesQuotation, String> {

    List<SalesQuotation> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    List<SalesQuotation> findByStatusOrderByCreatedAtDesc(QuotationStatus status);

    List<SalesQuotation> findAllByOrderByCreatedAtDesc();

    long countByQuotationNumberStartingWith(String prefix);

    /**
     * 2026-09-07 remediation: {@code ExecutiveAnalyticsService.getExecutiveOverview}'s
     * "Sales Bookings" figure used to sum EVERY quotation the tenant has ever created regardless
     * of the requested period — a period-response field that never actually varied by period.
     * Scopes it to a real date range, pushed into SQL. Supported by {@code idx_sales_quotations_app_quote_date}.
     */
    List<SalesQuotation> findByQuoteDateBetween(LocalDate start, LocalDate end);
}
