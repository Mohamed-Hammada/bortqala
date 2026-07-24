package com.bemo.hr.operations;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

interface InventoryItemRepository extends JpaRepository<InventoryItem, String> {
    List<InventoryItem> findAllByOrderByNameAsc();
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, String id);
}

interface StockMovementRepository extends JpaRepository<StockMovement, String> {
    List<StockMovement> findAllByOrderByOccurredAtDesc();
    @Query("select coalesce(sum(m.quantityDelta), 0) from StockMovement m where m.itemId = :itemId")
    BigDecimal balance(String itemId);
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
