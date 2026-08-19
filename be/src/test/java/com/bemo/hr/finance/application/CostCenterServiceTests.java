package com.bemo.hr.finance.application;

import com.bemo.hr.finance.application.CostCenterService.CostCenterPayload;
import com.bemo.hr.finance.application.CostCenterService.CostCenterResponse;
import com.bemo.hr.finance.domain.CostCenter;
import com.bemo.hr.finance.infrastructure.CostCenterRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CostCenterServiceTests {

    @Mock
    private CostCenterRepository costCenterRepository;

    private CostCenterService service;

    @BeforeEach
    void setUp() {
        service = new CostCenterService(costCenterRepository);
    }

    @Test
    @DisplayName("Creates cost center successfully when valid")
    void testCreateCostCenter() {
        CostCenterPayload payload = new CostCenterPayload(
                "CC-ENG-01", "Engineering Division", null, "user-1",
                false, true, 1700000000000L, null, "DIRECT_LABOR"
        );

        when(costCenterRepository.existsByCodeIgnoreCase("CC-ENG-01")).thenReturn(false);
        when(costCenterRepository.save(any(CostCenter.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CostCenterResponse response = service.create(payload);

        assertThat(response).isNotNull();
        assertThat(response.code()).isEqualTo("CC-ENG-01");
        assertThat(response.name()).isEqualTo("Engineering Division");
        assertThat(response.glAllocationRule()).isEqualTo("DIRECT_LABOR");
    }

    @Test
    @DisplayName("Throws exception on duplicate cost center code")
    void testDuplicateCostCenterCode() {
        CostCenterPayload payload = new CostCenterPayload(
                "CC-01", "Administration", null, null, false, true, null, null, null
        );

        when(costCenterRepository.existsByCodeIgnoreCase("CC-01")).thenReturn(true);

        assertThatThrownBy(() -> service.create(payload))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("مستخدم بالفعل");
    }

    @Test
    @DisplayName("Lists all active cost centers")
    void testListActiveCostCenters() {
        CostCenter cc1 = new CostCenter("CC-01", "Projects", null, null, true, true, null, null, null);
        when(costCenterRepository.findByActiveTrueOrderByCodeAsc()).thenReturn(List.of(cc1));

        List<CostCenterResponse> list = service.listActive();
        assertThat(list).hasSize(1);
        assertThat(list.get(0).code()).isEqualTo("CC-01");
    }
}
