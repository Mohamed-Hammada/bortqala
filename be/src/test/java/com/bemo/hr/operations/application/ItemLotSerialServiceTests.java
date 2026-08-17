package com.bemo.hr.operations.application;

import com.bemo.hr.operations.domain.ItemLotSerial;
import com.bemo.hr.operations.infrastructure.ItemLotSerialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
}
