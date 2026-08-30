package com.bemo.hr.trade.sales.infrastructure;

import com.bemo.hr.trade.sales.domain.QuotationStatus;
import com.bemo.hr.trade.sales.domain.SalesQuotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalesQuotationRepository extends JpaRepository<SalesQuotation, String> {

    List<SalesQuotation> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    List<SalesQuotation> findByStatusOrderByCreatedAtDesc(QuotationStatus status);

    List<SalesQuotation> findAllByOrderByCreatedAtDesc();

    long countByQuotationNumberStartingWith(String prefix);
}
