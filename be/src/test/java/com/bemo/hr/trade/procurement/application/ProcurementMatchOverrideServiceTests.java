package com.bemo.hr.trade.procurement.application;

import com.bemo.hr.trade.procurement.domain.ProcurementMatchOverride;
import com.bemo.hr.trade.procurement.infrastructure.ProcurementMatchOverrideRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProcurementMatchOverrideServiceTests {

    private ProcurementMatchOverrideRepository repository;
    private ProcurementMatchOverrideService service;

    @BeforeEach
    void setUp() {
        repository = mock(ProcurementMatchOverrideRepository.class);
        service = new ProcurementMatchOverrideService(repository);
    }

    @Test
    void approvesMatchOverrideSuccessfully() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProcurementMatchOverride override = service.approveOverride("match-55", "Price variance within tolerance manager threshold", "fin_mgr");
        assertThat(override).isNotNull();
        assertThat(override.getMatchId()).isEqualTo("match-55");
        assertThat(override.getApprovedBy()).isEqualTo("fin_mgr");
        assertThat(override.getStatus()).isEqualTo(ProcurementMatchOverride.Status.APPROVED);

        when(repository.findByMatchId("match-55")).thenReturn(Optional.of(override));
        assertThat(service.getOverride("match-55")).isNotNull();
    }
}
