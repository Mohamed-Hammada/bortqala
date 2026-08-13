package com.bemo.hr.operations.infrastructure;

import com.bemo.hr.operations.domain.ItemLotSerial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemLotSerialRepository extends JpaRepository<ItemLotSerial, String> {
    List<ItemLotSerial> findByItemId(String itemId);
    List<ItemLotSerial> findByItemIdAndStatus(String itemId, ItemLotSerial.Status status);
    Optional<ItemLotSerial> findBySerialNumberIgnoreCase(String serialNumber);
    Optional<ItemLotSerial> findByItemIdAndWarehouseIdAndLotNumberIgnoreCase(String itemId, String warehouseId, String lotNumber);
}
