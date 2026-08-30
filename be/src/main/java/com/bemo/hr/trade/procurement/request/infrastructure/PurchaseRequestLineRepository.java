package com.bemo.hr.trade.procurement.request.infrastructure;

import com.bemo.hr.trade.procurement.request.domain.PurchaseRequestLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseRequestLineRepository extends JpaRepository<PurchaseRequestLine, String> {

    List<PurchaseRequestLine> findByRequestIdOrderByItemNameAsc(String requestId);

    @Modifying
    void deleteByRequestId(String requestId);
}
