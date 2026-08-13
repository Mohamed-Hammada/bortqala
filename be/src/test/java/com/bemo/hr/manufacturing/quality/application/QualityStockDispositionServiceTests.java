package com.bemo.hr.manufacturing.quality.application;

import com.bemo.hr.manufacturing.quality.domain.QualityStockDisposition;
import com.bemo.hr.manufacturing.quality.infrastructure.QualityStockDispositionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class QualityStockDispositionServiceTests {

    private QualityStockDispositionRepository repository;
    private QualityStockDispositionService service;

    @BeforeEach
    void setUp() {
        repository = mock(QualityStockDispositionRepository.class);
        service = new QualityStockDispositionService(repository);
    }

    @Test
    void createsQualityStockDispositionSuccessfully() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        QualityStockDisposition disposition = service.createDisposition("insp-101", "SCRAP_QUARANTINE", new BigDecimal("5.0000"), "Substandard surface finish");
        assertThat(disposition).isNotNull();
        assertThat(disposition.getInspectionId()).isEqualTo("insp-101");
        assertThat(disposition.getQuantity()).isEqualByComparingTo(new BigDecimal("5.0000"));
        assertThat(disposition.getStatus()).isEqualTo(QualityStockDisposition.Status.DISPOSED);

        when(repository.findByInspectionId("insp-101")).thenReturn(List.of(disposition));
        assertThat(service.getDispositionsForInspection("insp-101")).hasSize(1);
    }
}
