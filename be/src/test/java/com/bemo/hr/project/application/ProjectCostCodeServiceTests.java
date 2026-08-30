package com.bemo.hr.project.application;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.project.api.ProjectApi.*;
import com.bemo.hr.project.domain.CostCodeCategory;
import com.bemo.hr.project.domain.ProjectCostCode;
import com.bemo.hr.project.infrastructure.ProjectCostCodeRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProjectCostCodeServiceTests {

    private ProjectCostCodeRepository repository;
    private AuditService auditService;
    private ProjectCostCodeService service;

    @BeforeEach
    void setUp() {
        repository = mock(ProjectCostCodeRepository.class);
        auditService = mock(AuditService.class);
        service = new ProjectCostCodeService(repository, auditService);
    }

    @Test
    void createCostCode_succeeds_whenValid() {
        when(repository.existsByCode("CC-LAB-01")).thenReturn(false);
        when(repository.save(any(ProjectCostCode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateCostCodeRequest request = new CreateCostCodeRequest(
                "CC-LAB-01", "عمالة خرسانات مسلحة", "Reinforced Concrete Labor",
                CostCodeCategory.LABOR, "أجور عمالة صب الخرسانات"
        );

        ProjectCostCodeResponse response = service.createCostCode(request);

        assertThat(response).isNotNull();
        assertThat(response.code()).isEqualTo("CC-LAB-01");
        assertThat(response.category()).isEqualTo(CostCodeCategory.LABOR);
        assertThat(response.active()).isTrue();
    }

    @Test
    void createCostCode_throwsConflict_whenDuplicate() {
        when(repository.existsByCode("CC-LAB-01")).thenReturn(true);

        CreateCostCodeRequest request = new CreateCostCodeRequest(
                "CC-LAB-01", "عمالة خرسانات", null, CostCodeCategory.LABOR, null
        );

        assertThatThrownBy(() -> service.createCostCode(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Cost code is already in use.");
    }
}
