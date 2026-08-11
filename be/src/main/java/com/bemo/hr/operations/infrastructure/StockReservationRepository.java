package com.bemo.hr.operations.infrastructure;

import com.bemo.hr.operations.domain.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, String> {
    List<StockReservation> findByWarehouseIdAndItemIdAndStatus(String warehouseId, String itemId, StockReservation.Status status);
}
