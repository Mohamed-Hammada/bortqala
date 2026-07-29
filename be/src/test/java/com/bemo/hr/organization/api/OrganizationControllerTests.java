package com.bemo.hr.organization.api;

import com.bemo.hr.organization.domain.Company;
import com.bemo.hr.organization.infrastructure.BranchRepository;
import com.bemo.hr.organization.infrastructure.CompanyRepository;
import com.bemo.hr.organization.infrastructure.DepartmentRepository;
import com.bemo.hr.organization.infrastructure.WarehouseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void readingAnEmptyHierarchyDoesNotCreateDemoOrganizationData() {
        when(companyRepository.findAllByOrderByCodeAsc()).thenReturn(List.of());
        when(branchRepository.findAllByOrderByCodeAsc()).thenReturn(List.of());
        when(warehouseRepository.findAllByOrderByCodeAsc()).thenReturn(List.of());
        when(departmentRepository.findAllByOrderByCodeAsc()).thenReturn(List.of());
        var controller = new OrganizationController(
                companyRepository, branchRepository, warehouseRepository, departmentRepository);

        var result = controller.getHierarchy();

        assertThat(result.companies()).isEmpty();
        assertThat(result.branches()).isEmpty();
        assertThat(result.warehouses()).isEmpty();
        assertThat(result.departments()).isEmpty();
        verify(companyRepository, never()).save(any(Company.class));
    }
}
