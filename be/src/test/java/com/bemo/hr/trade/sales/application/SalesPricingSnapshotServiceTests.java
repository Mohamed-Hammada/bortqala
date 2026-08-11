package com.bemo.hr.trade.sales.application;

import com.bemo.hr.trade.sales.domain.SalesPricingSnapshot;
import com.bemo.hr.trade.sales.infrastructure.SalesPricingSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SalesPricingSnapshotServiceTests {

    private SalesPricingSnapshotRepository snapshotRepository;
    private SalesPricingSnapshotService snapshotService;

    @BeforeEach
    void setUp() {
        snapshotRepository = mock(SalesPricingSnapshotRepository.class);
        snapshotService = new SalesPricingSnapshotService(snapshotRepository);
    }

    @Test
    void freezesPricingSnapshotWithDiscount() {
        when(snapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SalesPricingSnapshot snapshot = snapshotService.freezePricingSnapshot("so-100", "item-10", new BigDecimal("200.00"), new BigDecimal("10.00"));

        assertThat(snapshot).isNotNull();
        assertThat(snapshot.getSalesOrderId()).isEqualTo("so-100");
        assertThat(snapshot.getUnitPrice()).isEqualTo(new BigDecimal("200.00"));
        assertThat(snapshot.getNetPrice()).isEqualByComparingTo(new BigDecimal("180.00"));
    }
}
