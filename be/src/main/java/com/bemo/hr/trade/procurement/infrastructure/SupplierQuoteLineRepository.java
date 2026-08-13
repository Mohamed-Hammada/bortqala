package com.bemo.hr.trade.procurement.infrastructure;

import com.bemo.hr.trade.procurement.domain.SupplierQuoteLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierQuoteLineRepository extends JpaRepository<SupplierQuoteLine, String> {
    List<SupplierQuoteLine> findByQuoteId(String quoteId);
}
