package com.bemo.hr.operations.infrastructure;

import com.bemo.hr.operations.domain.ItemLotSerial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ItemLotSerialRepository extends JpaRepository<ItemLotSerial, String> {
    List<ItemLotSerial> findByItemId(String itemId);

    List<ItemLotSerial> findByItemIdAndStatus(String itemId, ItemLotSerial.Status status);

    Optional<ItemLotSerial> findBySerialNumberIgnoreCase(String serialNumber);

    Optional<ItemLotSerial> findByItemIdAndWarehouseIdAndLotNumberIgnoreCase(String itemId, String warehouseId, String lotNumber);

    /**
     * FEFO picker: returns available lots for an item+warehouse ordered by expiry date ascending
     * (earliest expiry first). Lots without expiry go last. Expired lots are excluded.
     */
    @Query("SELECT l FROM ItemLotSerial l WHERE l.itemId = :itemId " +
           "AND l.warehouseId = :warehouseId AND l.status = 'AVAILABLE' AND l.quantity > 0 " +
           "AND (l.expirationDate IS NULL OR l.expirationDate > CURRENT_DATE) " +
           "ORDER BY l.expirationDate ASC NULLS LAST, l.createdAt ASC")
    List<ItemLotSerial> findFefoLots(@Param("itemId") String itemId, @Param("warehouseId") String warehouseId);

    /**
     * FIFO picker: returns available lots for an item+warehouse ordered by creation date ascending.
     */
    @Query("SELECT l FROM ItemLotSerial l WHERE l.itemId = :itemId " +
           "AND l.warehouseId = :warehouseId AND l.status = 'AVAILABLE' AND l.quantity > 0 " +
           "ORDER BY l.createdAt ASC")
    List<ItemLotSerial> findFifoLots(@Param("itemId") String itemId, @Param("warehouseId") String warehouseId);

    /**
     * Find available lots expiring within N days (for expiry warnings).
     */
    @Query("SELECT l FROM ItemLotSerial l WHERE l.itemId = :itemId " +
           "AND l.warehouseId = :warehouseId AND l.status = 'AVAILABLE' AND l.quantity > 0 " +
           "AND l.expirationDate IS NOT NULL AND l.expirationDate <= CURRENT_DATE + :days " +
           "AND l.expirationDate > CURRENT_DATE " +
           "ORDER BY l.expirationDate ASC")
    List<ItemLotSerial> findLotsExpiringWithinDays(@Param("itemId") String itemId, @Param("warehouseId") String warehouseId, @Param("days") int days);

    /**
     * Find all available lots for an item (across all warehouses).
     */
    @Query("SELECT l FROM ItemLotSerial l WHERE l.itemId = :itemId " +
           "AND l.status = 'AVAILABLE' AND l.quantity > 0 " +
           "ORDER BY l.expirationDate ASC NULLS LAST")
    List<ItemLotSerial> findAvailableLotsByItem(@Param("itemId") String itemId);
}
