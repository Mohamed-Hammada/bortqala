package com.bemo.hr.operations;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface StockMovementRepository extends JpaRepository<StockMovement, String> {
    List<StockMovement> findAllByOrderByOccurredAtDesc();

    Optional<StockMovement> findFirstByItemIdOrderByOccurredAtDesc(String itemId);

    List<StockMovement> findByItemIdOrderByOccurredAtDesc(String itemId);

    List<StockMovement> findByProjectIdOrderByOccurredAtDesc(String projectId);

    @Query("select coalesce(sum(m.quantityDelta), 0) from StockMovement m where m.itemId = :itemId")
    BigDecimal balance(String itemId);

    @Query("select coalesce(sum(m.quantityDelta), 0) from StockMovement m where m.itemId = :itemId and m.occurredAt <= :asOf")
    BigDecimal balanceAsOf(String itemId, java.time.Instant asOf);

    @Query("select coalesce(sum(m.quantityDelta), 0) from StockMovement m where m.itemId = :itemId and m.warehouse = :warehouse")
    BigDecimal balanceByWarehouse(String itemId, String warehouse);

    @Query("select coalesce(sum(m.quantityDelta), 0) from StockMovement m where m.itemId = :itemId and m.warehouse = :warehouse and m.occurredAt <= :asOf")
    BigDecimal balanceAsOfAndWarehouse(String itemId, String warehouse, java.time.Instant asOf);

    @Query("select m.itemId, coalesce(sum(m.quantityDelta), 0) from StockMovement m group by m.itemId having coalesce(sum(m.quantityDelta), 0) < 0")
    List<Object[]> findNegativeBalanceItemIds();

    boolean existsByPartyIdAndInvoiceNoIgnoreCase(String partyId, String invoiceNo);

    List<StockMovement> findByOperationTypeAndReferenceCodeAndItemId(
            String operationType, String referenceCode, String itemId);
}
