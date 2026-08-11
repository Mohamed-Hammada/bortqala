package com.bemo.hr.operations.application;

import com.bemo.hr.operations.domain.StockStatusBalance;
import com.bemo.hr.operations.domain.StockValuationRecord;
import com.bemo.hr.operations.infrastructure.StockStatusBalanceRepository;
import com.bemo.hr.operations.infrastructure.StockValuationRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InventoryValuationSnapshotServiceTests {

    private StockStatusBalanceRepository balanceRepository;
    private StockValuationRecordRepository valuationRepository;
    private InventoryValuationSnapshotService valuationService;

    @BeforeEach
    void setUp() {
        balanceRepository = mock(StockStatusBalanceRepository.class);
        valuationRepository = mock(StockValuationRecordRepository.class);
        valuationService = new InventoryValuationSnapshotService(balanceRepository, valuationRepository);
    }

    @Test
    void calculatesValuationAndReconcilesWithGlBalanceSuccessfully() {
        StockStatusBalance b1 = new StockStatusBalance("wh-1", "bin-1", "item-1", StockStatusBalance.Status.AVAILABLE, new BigDecimal("10.0000"));
        when(balanceRepository.findAll()).thenReturn(List.of(b1));
        when(valuationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LocalDate date = LocalDate.of(2026, 8, 1);
        List<StockValuationRecord> records = valuationService.calculateValuation(date, new BigDecimal("50.0000"));

        assertThat(records).hasSize(1);
        assertThat(records.get(0).getTotalValue()).isEqualByComparingTo(new BigDecimal("500.00"));

        when(valuationRepository.findByAsOfDate(date)).thenReturn(records);

        InventoryValuationSnapshotService.ValuationReconciliationResult recon = valuationService.reconcileWithGeneralLedger(date, new BigDecimal("500.00"));
        assertThat(recon.inBalance()).isTrue();
        assertThat(recon.variance()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
