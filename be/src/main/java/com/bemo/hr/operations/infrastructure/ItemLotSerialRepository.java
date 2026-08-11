package com.bemo.hr.operations.infrastructure;

import com.bemo.hr.operations.domain.ItemLotSerial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemLotSerialRepository extends JpaRepository<ItemLotSerial, String> {
    List<ItemLotSerial> findByItemId(String itemId);
    List<ItemLotSerial> findByItemIdAndStatus(String itemId, ItemLotSerial.Status status);
}
