package com.bemo.hr.operations.application;

import com.bemo.hr.operations.domain.StockStatusBalance;
import com.bemo.hr.operations.domain.StockValuationRecord;
import com.bemo.hr.operations.infrastructure.StockStatusBalanceRepository;
import com.bemo.hr.operations.infrastructure.StockValuationRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class InventoryValuationSnapshotService {

    private final StockStatusBalanceRepository balanceRepository;
    private final StockValuationRecordRepository valuationRepository;

    public InventoryValuationSnapshotService(StockStatusBalanceRepository balanceRepository,
                                           StockValuationRecordRepository valuationRepository) {
        this.balanceRepository = balanceRepository;
        this.valuationRepository = valuationRepository;
    }

    public record ValuationReconciliationResult(
            LocalDate asOfDate,
            BigDecimal subledgerTotalValue,
            BigDecimal glAccountBalance,
            BigDecimal variance,
            boolean inBalance
    ) {}

    @Transactional
    public List<StockValuationRecord> calculateValuation(LocalDate asOfDate, BigDecimal defaultUnitCost) {
        List<StockStatusBalance> balances = balanceRepository.findAll();
        List<StockValuationRecord> records = new ArrayList<>();

        for (StockStatusBalance b : balances) {
            StockValuationRecord rec = new StockValuationRecord(b.getItemId(), b.getWarehouseId(), b.getQuantity(), defaultUnitCost, asOfDate);
            records.add(valuationRepository.save(rec));
        }
        return records;
    }

    @Transactional(readOnly = true)
    public ValuationReconciliationResult reconcileWithGeneralLedger(LocalDate asOfDate, BigDecimal glBalance) {
        List<StockValuationRecord> records = valuationRepository.findByAsOfDate(asOfDate);
        BigDecimal subledgerTotal = records.stream()
                .map(StockValuationRecord::getTotalValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal variance = subledgerTotal.subtract(glBalance);
        boolean inBalance = variance.compareTo(BigDecimal.ZERO) == 0;

        return new ValuationReconciliationResult(asOfDate, subledgerTotal, glBalance, variance, inBalance);
    }
}
