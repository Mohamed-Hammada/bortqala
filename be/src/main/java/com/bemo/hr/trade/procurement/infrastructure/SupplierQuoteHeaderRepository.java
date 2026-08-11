package com.bemo.hr.trade.procurement.infrastructure;

import com.bemo.hr.trade.procurement.domain.SupplierQuoteHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierQuoteHeaderRepository extends JpaRepository<SupplierQuoteHeader, String> {
    List<SupplierQuoteHeader> findByRfqId(String rfqId);
    List<SupplierQuoteHeader> findBySupplierId(String supplierId);
}
