package com.bemo.hr.trade.procurement.infrastructure;

import com.bemo.hr.trade.procurement.domain.SupplierReturn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierReturnRepository extends JpaRepository<SupplierReturn, String> {
    List<SupplierReturn> findAllByOrderByReturnDateDesc();

    List<SupplierReturn> findByPurchaseOrderId(String purchaseOrderId);

    boolean existsByReturnNumberIgnoreCase(String returnNumber);
}
