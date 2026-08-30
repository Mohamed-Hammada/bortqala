package com.bemo.hr.organization.api;

import com.bemo.hr.organization.application.IntercompanyService;
import com.bemo.hr.organization.domain.Company;
import com.bemo.hr.organization.infrastructure.BranchRepository;
import com.bemo.hr.organization.infrastructure.CompanyRepository;
import com.bemo.hr.organization.infrastructure.DepartmentRepository;
import com.bemo.hr.organization.infrastructure.WarehouseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationControllerTests {
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private BranchRepository branchRepository;
    @Mock
    private WarehouseRepository warehouseRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private IntercompanyService intercompanyService;

    @Test
    void readingAnEmptyHierarchyDoesNotCreateDemoOrganizationData() {
        when(companyRepository.findAllByOrderByCodeAsc()).thenReturn(List.of());
        when(branchRepository.findAllByOrderByCodeAsc()).thenReturn(List.of());
        when(warehouseRepository.findAllByOrderByCodeAsc()).thenReturn(List.of());
        when(departmentRepository.findAllByOrderByCodeAsc()).thenReturn(List.of());
        var controller = new OrganizationController(
                companyRepository, branchRepository, warehouseRepository, departmentRepository, intercompanyService);

        var result = controller.getHierarchy();

        assertThat(result.companies()).isEmpty();
        assertThat(result.branches()).isEmpty();
        assertThat(result.warehouses()).isEmpty();
        assertThat(result.departments()).isEmpty();
        verify(companyRepository, never()).save(any(Company.class));
    }

    @Test
    void getConsolidatedSummaryDelegatesToService() {
        OrganizationApi.ConsolidatedOrganizationSummary summary = new OrganizationApi.ConsolidatedOrganizationSummary(
                BigDecimal.valueOf(1_000_000),
                BigDecimal.valueOf(600_000),
                BigDecimal.valueOf(50_000),
                BigDecimal.valueOf(400_000),
                2,
                30,
                List.of()
        );
        when(intercompanyService.getConsolidatedSummary()).thenReturn(summary);
        var controller = new OrganizationController(
                companyRepository, branchRepository, warehouseRepository, departmentRepository, intercompanyService);

        var result = controller.getConsolidatedSummary();

        assertThat(result.totalRevenue()).isEqualByComparingTo(BigDecimal.valueOf(1_000_000));
        assertThat(result.activeBranches()).isEqualTo(2);
        verify(intercompanyService).getConsolidatedSummary();
    }
}
