package com.bemo.hr.operations;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

interface InventoryItemRepository extends JpaRepository<InventoryItem, String> {
    List<InventoryItem> findAllByOrderByNameAsc();
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, String id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InventoryItem i where i.id = :id")
    Optional<InventoryItem> findByIdForUpdate(String id);
}

interface InventoryValuationPolicyRepository extends JpaRepository<InventoryValuationPolicy, String> {
    Optional<InventoryValuationPolicy> findByAppId(String appId);
}

interface InventoryCostLayerRepository extends JpaRepository<InventoryCostLayer, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from InventoryCostLayer l where l.itemId = :itemId and l.remainingQuantity > 0 order by l.receivedAt, l.createdAt")
    List<InventoryCostLayer> findOpenForUpdate(String itemId);
    List<InventoryCostLayer> findByItemIdOrderByReceivedAtAscCreatedAtAsc(String itemId);
}

interface InventoryMovementCostRepository extends JpaRepository<InventoryMovementCost, String> {
    Optional<InventoryMovementCost> findByMovementId(String movementId);
    List<InventoryMovementCost> findAllByOrderByOccurredAtDesc();
    List<InventoryMovementCost> findByItemIdOrderByOccurredAtAsc(String itemId);
    boolean existsByItemIdAndOccurredAtAfter(String itemId, Instant occurredAt);
    @Query("select coalesce(sum(c.valueEffect), 0) from InventoryMovementCost c where c.itemId = :itemId")
    BigDecimal inventoryValue(String itemId);
    @Query("select coalesce(sum(c.quantityEffect), 0) from InventoryMovementCost c where c.itemId = :itemId")
    BigDecimal valuedQuantity(String itemId);
}

interface InventoryRevaluationRepository extends JpaRepository<InventoryRevaluation, String> {
    Optional<InventoryRevaluation> findByOperationId(String operationId);
    List<InventoryRevaluation> findByItemIdOrderByOccurredAtDesc(String itemId);
    @Query("select coalesce(sum(r.valueDifference), 0) from InventoryRevaluation r where r.itemId = :itemId")
    BigDecimal revaluationValue(String itemId);
}

interface StockMovementRepository extends JpaRepository<StockMovement, String> {
    List<StockMovement> findAllByOrderByOccurredAtDesc();
    @Query("select coalesce(sum(m.quantityDelta), 0) from StockMovement m where m.itemId = :itemId")
    BigDecimal balance(String itemId);
    @Query("select m.itemId, coalesce(sum(m.quantityDelta), 0) from StockMovement m group by m.itemId having coalesce(sum(m.quantityDelta), 0) < 0")
    List<Object[]> findNegativeBalanceItemIds();
    boolean existsByPartyIdAndInvoiceNoIgnoreCase(String partyId, String invoiceNo);
}

interface EmployeeAdvanceEntryRepository extends JpaRepository<EmployeeAdvanceEntry, String> {
    List<EmployeeAdvanceEntry> findAllByOrderByOccurredAtDesc();
    @Query("select coalesce(sum(e.amountDelta), 0) from EmployeeAdvanceEntry e where e.employeeId = :employeeId")
    BigDecimal balance(String employeeId);
}

interface ItemCategoryRepository extends JpaRepository<ItemCategory, String> {
    Optional<ItemCategory> findByNameAndAppId(String name, String appId);
    List<ItemCategory> findByActiveTrueAndAppIdOrderByNameAsc(String appId);
    List<ItemCategory> findByAppIdOrderByNameAsc(String appId);
}

interface UnitOfMeasureRepository extends JpaRepository<UnitOfMeasure, String> {
    Optional<UnitOfMeasure> findByNameAndAppId(String name, String appId);
    List<UnitOfMeasure> findByActiveTrueAndAppIdOrderByNameAsc(String appId);
    List<UnitOfMeasure> findByAppIdOrderByNameAsc(String appId);
}

interface UnitConversionRepository extends JpaRepository<UnitConversion, String> {
    List<UnitConversion> findByFromUomIdOrToUomIdOrderByFromUomId(String fromUomId, String toUomId);
    Optional<UnitConversion> findByFromUomIdAndToUomId(String fromUomId, String toUomId);
    List<UnitConversion> findAllByOrderByFromUomId();
}
