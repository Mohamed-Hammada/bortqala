package com.bemo.hr.manufacturing.production.application;

import com.bemo.hr.manufacturing.production.domain.BomSnapshot;
import com.bemo.hr.manufacturing.production.infrastructure.BomSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BomSnapshotServiceTests {

    private BomSnapshotRepository bomSnapshotRepository;
    private BomSnapshotService bomSnapshotService;

    @BeforeEach
    void setUp() {
        bomSnapshotRepository = mock(BomSnapshotRepository.class);
        bomSnapshotService = new BomSnapshotService(bomSnapshotRepository);
    }

    @Test
    void capturesBomSnapshotSuccessfully() {
        when(bomSnapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BomSnapshot snapshot = bomSnapshotService.captureBomSnapshot("po-100", "bom-1", 1, "comp-5",
                new BigDecimal("50.0000"), new BigDecimal("12.500000"));

        assertThat(snapshot).isNotNull();
        assertThat(snapshot.getProductionOrderId()).isEqualTo("po-100");
        assertThat(snapshot.getBomId()).isEqualTo("bom-1");
        assertThat(snapshot.getRequiredQuantity()).isEqualTo(new BigDecimal("50.0000"));
        assertThat(snapshot.getStandardUnitCost()).isEqualByComparingTo("12.500000");
    }
}
