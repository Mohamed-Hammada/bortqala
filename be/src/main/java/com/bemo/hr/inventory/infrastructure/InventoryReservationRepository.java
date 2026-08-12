package com.bemo.hr.inventory.infrastructure;

import com.bemo.hr.inventory.domain.InventoryReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, String> {
    List<InventoryReservation> findByItemIdAndWarehouseIdAndStatus(String itemId, String warehouseId, InventoryReservation.Status status);
}
