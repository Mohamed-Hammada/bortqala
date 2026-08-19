package com.bemo.hr.trade.sales.infrastructure;

import com.bemo.hr.trade.sales.domain.SalesQuotationLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalesQuotationLineRepository extends JpaRepository<SalesQuotationLine, String> {

    List<SalesQuotationLine> findByQuotationId(String quotationId);

    void deleteByQuotationId(String quotationId);
}
