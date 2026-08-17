package com.bemo.hr.manufacturing.production.application;

import com.bemo.hr.manufacturing.production.domain.MaterialReservationHeader;
import com.bemo.hr.manufacturing.production.domain.MaterialReservationLine;
import com.bemo.hr.manufacturing.production.domain.WipPostingRecord;
import com.bemo.hr.manufacturing.production.infrastructure.MaterialReservationHeaderRepository;
import com.bemo.hr.manufacturing.production.infrastructure.MaterialReservationLineRepository;
import com.bemo.hr.manufacturing.production.infrastructure.WipPostingRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ManufacturingWipServiceTests {

    private MaterialReservationHeaderRepository reservationHeaderRepository;
    private MaterialReservationLineRepository reservationLineRepository;
    private WipPostingRecordRepository wipPostingRepository;
    private ManufacturingWipService wipService;

    @BeforeEach
    void setUp() {
        reservationHeaderRepository = mock(MaterialReservationHeaderRepository.class);
        reservationLineRepository = mock(MaterialReservationLineRepository.class);
        wipPostingRepository = mock(WipPostingRecordRepository.class);
        wipService = new ManufacturingWipService(reservationHeaderRepository, reservationLineRepository, wipPostingRepository);
    }

    @Test
    void createsReservationAddsLineAndPostsWipSuccessfully() {
        when(reservationHeaderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(reservationLineRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(wipPostingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MaterialReservationHeader header = wipService.createReservation("wo-101");
        assertThat(header).isNotNull();
        assertThat(header.getStatus()).isEqualTo(MaterialReservationHeader.Status.ACTIVE);

        MaterialReservationLine line = wipService.addReservationLine(header.getId(), "item-10", new BigDecimal("25.0000"));
        assertThat(line.getReservedQuantity()).isEqualByComparingTo(new BigDecimal("25.0000"));

        WipPostingRecord wip = wipService.postWip("wo-101", "wc-1", new BigDecimal("8.00"), new BigDecimal("4.00"), new BigDecimal("1200.00"));
        assertThat(wip).isNotNull();
        assertThat(wip.getStatus()).isEqualTo(WipPostingRecord.Status.POSTED);
    }
}
