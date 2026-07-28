package com.bemo.hr.operations;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

interface InventoryItemRepository extends JpaRepository<InventoryItem, String> {
    List<InventoryItem> findAllByOrderByNameAsc();
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, String id);
}

interface StockMovementRepository extends JpaRepository<StockMovement, String> {
    List<StockMovement> findAllByOrderByOccurredAtDesc();
    @Query("select coalesce(sum(m.quantityDelta), 0) from StockMovement m where m.itemId = :itemId")
    BigDecimal balance(String itemId);
    @Query("select m.itemId, coalesce(sum(m.quantityDelta), 0) from StockMovement m group by m.itemId having coalesce(sum(m.quantityDelta), 0) < 0")
    List<Object[]> findNegativeBalanceItemIds();
}

interface PartnerLedgerEntryRepository extends JpaRepository<PartnerLedgerEntry, String> {
    List<PartnerLedgerEntry> findAllByOrderByOccurredAtDesc();
    List<PartnerLedgerEntry> findByPartyIdOrderByOccurredAtDesc(String partyId);
    @Query("select coalesce(sum(e.amountDelta), 0) from PartnerLedgerEntry e where e.partyId = :partyId")
    BigDecimal balance(String partyId);
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
