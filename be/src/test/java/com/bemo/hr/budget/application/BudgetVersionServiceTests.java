package com.bemo.hr.budget.application;

import com.bemo.hr.budget.domain.BudgetVersion;
import com.bemo.hr.budget.infrastructure.BudgetVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BudgetVersionServiceTests {

    private BudgetVersionRepository repository;
    private BudgetVersionService service;

    @BeforeEach
    void setUp() {
        repository = mock(BudgetVersionRepository.class);
        service = new BudgetVersionService(repository);
    }

    @Test
    void createsAndActivatesBudgetVersionSupersedingOlderVersions() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BudgetVersion v1 = service.createVersion("FY2026-V1", "FY2026 Initial Budget", 2026);
        assertThat(v1).isNotNull();
        assertThat(v1.getStatus()).isEqualTo(BudgetVersion.Status.DRAFT);

        when(repository.findById(v1.getId())).thenReturn(Optional.of(v1));
        when(repository.findByFiscalYear(2026)).thenReturn(List.of(v1));

        BudgetVersion activated = service.activateVersion(v1.getId());
        assertThat(activated.getStatus()).isEqualTo(BudgetVersion.Status.ACTIVE);
    }
}
