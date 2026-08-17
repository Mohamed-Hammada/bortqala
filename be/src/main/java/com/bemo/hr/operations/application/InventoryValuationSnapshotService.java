package com.bemo.hr.operations.application;

import com.bemo.hr.operations.domain.StockStatusBalance;
import com.bemo.hr.operations.domain.StockValuationRecord;
import com.bemo.hr.operations.infrastructure.StockStatusBalanceRepository;
import com.bemo.hr.operations.infrastructure.StockValuationRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class InventoryValuationSnapshotService {

    private final StockStatusBalanceRepository balanceRepository;
    private final StockValuationRecordRepository valuationRepository;

    public InventoryValuationSnapshotService(StockStatusBalanceRepository balanceRepository,
                                             StockValuationRecordRepository valuationRepository) {
        this.balanceRepository = balanceRepository;
        this.valuationRepository = valuationRepository;
    }

    @Transactional
    public List<StockValuationRecord> calculateValuation(LocalDate asOfDate, BigDecimal defaultUnitCost) {
        log.debug("calculateValuation called with asOfDate={}, defaultUnitCost={}", asOfDate, defaultUnitCost);
        List<StockStatusBalance> balances = balanceRepository.findAll();
        List<StockValuationRecord> records = new ArrayList<>();

        for (StockStatusBalance b : balances) {
            StockValuationRecord rec = new StockValuationRecord(b.getItemId(), b.getWarehouseId(), b.getQuantity(), defaultUnitCost, asOfDate);
            records.add(valuationRepository.save(rec));
        }
        log.info("Valuation calculated for {} items as of {}", records.size(), asOfDate);
        return records;
    }

    @Transactional(readOnly = true)
    public ValuationReconciliationResult reconcileWithGeneralLedger(LocalDate asOfDate, BigDecimal glBalance) {
        log.debug("reconcileWithGeneralLedger called with asOfDate={}, glBalance={}", asOfDate, glBalance);
        List<StockValuationRecord> records = valuationRepository.findByAsOfDate(asOfDate);
        BigDecimal subledgerTotal = records.stream()
                .map(StockValuationRecord::getTotalValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal variance = subledgerTotal.subtract(glBalance);
        boolean inBalance = variance.compareTo(BigDecimal.ZERO) == 0;

        log.info("Valuation reconciliation for asOfDate={}: subledger={}, gl={}, variance={}, inBalance={}", asOfDate, subledgerTotal, glBalance, variance, inBalance);
        return new ValuationReconciliationResult(asOfDate, subledgerTotal, glBalance, variance, inBalance);
    }

    public record ValuationReconciliationResult(
            LocalDate asOfDate,
            BigDecimal subledgerTotalValue,
            BigDecimal glAccountBalance,
            BigDecimal variance,
            boolean inBalance
    ) {
    }
}
