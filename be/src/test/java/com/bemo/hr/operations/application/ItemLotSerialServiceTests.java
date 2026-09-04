package com.bemo.hr.operations.application;

import com.bemo.hr.operations.domain.ItemLotSerial;
import com.bemo.hr.operations.infrastructure.ItemLotSerialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ItemLotSerialServiceTests {

    private ItemLotSerialRepository repository;
    private ItemLotSerialService service;

    @BeforeEach
    void setUp() {
        repository = mock(ItemLotSerialRepository.class);
        service = new ItemLotSerialService(repository);
    }

    @Test
    void createsQuarantinesAndBlocksLotSerialSuccessfully() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ItemLotSerial item = service.createLotSerial("item-1", "LOT-100", "SN-999", LocalDate.of(2027, 12, 31), LocalDate.of(2026, 1, 1));
        assertThat(item).isNotNull();
        assertThat(item.getStatus()).isEqualTo(ItemLotSerial.Status.AVAILABLE);

        when(repository.findById(item.getId())).thenReturn(Optional.of(item));

        service.quarantine(item.getId());
        assertThat(item.getStatus()).isEqualTo(ItemLotSerial.Status.QUARANTINED);

        service.block(item.getId());
        assertThat(item.getStatus()).isEqualTo(ItemLotSerial.Status.BLOCKED);
    }

    @Test
    void receivesIssuesAndReturnsLotWithDocumentTrace() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ItemLotSerial lot = service.receive("item-1", "wh-1", "LOT-200", null, new BigDecimal("5"),
                "GRN-1", null, null);
        when(repository.findById(lot.getId())).thenReturn(Optional.of(lot));

        service.issue(lot.getId(), new BigDecimal("2"), "DN-1");
        service.receiveReturn(lot.getId(), BigDecimal.ONE, "RET-1");

        assertThat(lot.getQuantity()).isEqualByComparingTo("4");
        assertThat(lot.getReceiptReference()).isEqualTo("GRN-1");
        assertThat(lot.getIssueReference()).isEqualTo("DN-1");
        assertThat(lot.getReturnReference()).isEqualTo("RET-1");
    }

    @Test
    void rejectsDuplicateSerialAndInvalidSerialQuantity() {
        ItemLotSerial existing = new ItemLotSerial("item-1", "LOT", "SER-1", null, null);
        when(repository.findBySerialNumberIgnoreCase("SER-1")).thenReturn(Optional.of(existing));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.receive("item-1", "wh-1", null, "SER-1",
                        BigDecimal.ONE, "GRN-1", null, null))
                .isInstanceOf(com.bemo.hr.shared.domain.BusinessRuleException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.receive("item-1", "wh-1", null, "SER-2",
                        new BigDecimal("2"), "GRN-1", null, null))
                .isInstanceOf(com.bemo.hr.shared.domain.BusinessRuleException.class);
    }

    @Test
    void pickFefoIssuesFromEarliestExpiringLotsFirst() {
        LocalDate today = LocalDate.now();
        ItemLotSerial lotA = new ItemLotSerial("item-1", "wh-1", "LOT-A", null, new BigDecimal("10"), "GRN-1", today.plusMonths(1), null);
        ItemLotSerial lotB = new ItemLotSerial("item-1", "wh-1", "LOT-B", null, new BigDecimal("10"), "GRN-2", today.plusMonths(3), null);
        ItemLotSerial lotC = new ItemLotSerial("item-1", "wh-1", "LOT-C", null, new BigDecimal("10"), "GRN-3", today.plusMonths(6), null);

        // FEFO order: A (earliest) -> B -> C
        when(repository.findFifoLots("item-1", "wh-1")).thenReturn(List.of(lotA, lotB, lotC));
        when(repository.findFefoLots("item-1", "wh-1")).thenReturn(List.of(lotA, lotB, lotC));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Pick 15 units: should take 10 from A + 5 from B
        List<ItemLotSerial> issued = service.pickFefo("item-1", "wh-1", new BigDecimal("15"), "DN-1");

        assertThat(issued).hasSize(2);
        assertThat(issued.get(0).getLotNumber()).isEqualTo("LOT-A");
        assertThat(issued.get(0).getQuantity()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(issued.get(1).getLotNumber()).isEqualTo("LOT-B");
        assertThat(issued.get(1).getQuantity()).isEqualByComparingTo(new BigDecimal("5"));
    }

    @Test
    void pickFefoFailsWhenInsufficientStock() {
        ItemLotSerial lotA = new ItemLotSerial("item-1", "wh-1", "LOT-A", null, new BigDecimal("3"), "GRN-1", null, null);
        when(repository.findFifoLots("item-1", "wh-1")).thenReturn(List.of(lotA));
        when(repository.findFefoLots("item-1", "wh-1")).thenReturn(List.of(lotA));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> service.pickFefo("item-1", "wh-1", new BigDecimal("10"), "DN-1"))
                .isInstanceOf(com.bemo.hr.shared.domain.BusinessRuleException.class);
    }

    @Test
    void pickFifoIssuesFromOldestLotsFirst() {
        ItemLotSerial lotOld = new ItemLotSerial("item-1", "wh-1", "LOT-OLD", null, new BigDecimal("5"), "GRN-1", null, null);
        ItemLotSerial lotNew = new ItemLotSerial("item-1", "wh-1", "LOT-NEW", null, new BigDecimal("5"), "GRN-2", null, null);

        when(repository.findFifoLots("item-1", "wh-1")).thenReturn(List.of(lotOld, lotNew));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<ItemLotSerial> issued = service.pickFifo("item-1", "wh-1", new BigDecimal("7"), "DN-2");

        assertThat(issued).hasSize(2);
        assertThat(issued.get(0).getLotNumber()).isEqualTo("LOT-OLD");
        assertThat(issued.get(0).getQuantity()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(issued.get(1).getLotNumber()).isEqualTo("LOT-NEW");
        assertThat(issued.get(1).getQuantity()).isEqualByComparingTo(new BigDecimal("3"));
    }

    @Test
    void rejectsPickWithNonPositiveQuantity() {
        assertThatThrownBy(() -> service.pickFefo("item-1", "wh-1", BigDecimal.ZERO, "DN-1"))
                .isInstanceOf(com.bemo.hr.shared.domain.BusinessRuleException.class);
        assertThatThrownBy(() -> service.pickFifo("item-1", "wh-1", new BigDecimal("-5"), "DN-1"))
                .isInstanceOf(com.bemo.hr.shared.domain.BusinessRuleException.class);
    }
}
