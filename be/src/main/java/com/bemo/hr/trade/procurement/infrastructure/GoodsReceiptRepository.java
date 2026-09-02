package com.bemo.hr.trade.procurement.infrastructure;

import com.bemo.hr.trade.procurement.domain.GoodsReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, String> {
    List<GoodsReceipt> findAllByOrderByReceiptDateDesc();

    List<GoodsReceipt> findByPurchaseOrderId(String purchaseOrderId);

    boolean existsByGrnNumberIgnoreCase(String grnNumber);

    @Query("select max(g.grnNumber) from GoodsReceipt g")
    Optional<String> findMaxGrnNumber();
}
