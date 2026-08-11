package com.bemo.hr.operations.application;

import com.bemo.hr.operations.domain.ItemLotSerial;
import com.bemo.hr.operations.infrastructure.ItemLotSerialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
}
