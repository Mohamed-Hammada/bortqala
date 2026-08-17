package com.bemo.hr.workforce;

import com.bemo.hr.employee.domain.AttendanceCategory;
import com.bemo.hr.employee.domain.AttendanceMode;
import com.bemo.hr.employee.domain.CategoryScope;
import com.bemo.hr.employee.domain.PayCycle;
import com.bemo.hr.employee.infrastructure.AttendanceCategoryRepository;
import com.bemo.hr.employee.infrastructure.EmployeeCodeSequenceRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WorkerCategoryServiceScopeTests {

    private final WorkerCategoryRepository categoryRepository = mock(WorkerCategoryRepository.class);
    private final AttendanceCategoryRepository attendanceCategoryRepository = mock(AttendanceCategoryRepository.class);
    private final EmployeeCodeSequenceRepository employeeCodeSequenceRepository = mock(EmployeeCodeSequenceRepository.class);
    private final WorkerCategoryService service =
            new WorkerCategoryService(categoryRepository, attendanceCategoryRepository, employeeCodeSequenceRepository);

    private static WorkerCategory persisted(WorkerCategory config) {
        java.time.Instant now = java.time.Instant.now();
        try {
            java.lang.reflect.Field createdAt = WorkerCategory.class.getDeclaredField("createdAt");
            java.lang.reflect.Field updatedAt = WorkerCategory.class.getDeclaredField("updatedAt");
            createdAt.setAccessible(true);
            updatedAt.setAccessible(true);
            createdAt.set(config, now);
            updatedAt.set(config, now);
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException(exception);
        }
        return config;
    }

    private WorkerCategory config(AttendanceCategory canonical) {
        WorkerCategory config = new WorkerCategory(canonical.getCode(), canonical.getName(), null,
                new BigDecimal("275.50"), new BigDecimal("8"), "HALF_MONTH", "ACTIVE");
        config.linkToCategory(canonical.getId());
        return persisted(config);
    }

    private AttendanceCategory canonical(String code, CategoryScope scope) {
        return new AttendanceCategory(code, code, 480, PayCycle.MONTHLY, AttendanceMode.MANUAL,
                false, 111, true, scope);
    }

    @Test
    void createWithWorkerScopePersistsACanonicalWorkerCategoryAndLinksTheConfig() {
        when(attendanceCategoryRepository.findByCodeIgnoreCase("WELD")).thenReturn(Optional.empty());
        when(attendanceCategoryRepository.save(any(AttendanceCategory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(categoryRepository.save(any(WorkerCategory.class)))
                .thenAnswer(invocation -> persisted(invocation.getArgument(0)));

        var response = service.create(new WorkforceApi.CategoryRequest("weld", "لحام", null,
                new BigDecimal("275.50"), new BigDecimal("8"), "HALF_MONTH", "ACTIVE", "WORKER"));

        assertThat(response.code()).isEqualTo("WELD");
        assertThat(response.scope()).isEqualTo("WORKER");
        verify(attendanceCategoryRepository).save(any(AttendanceCategory.class));
        verify(categoryRepository).save(any(WorkerCategory.class));
        verify(employeeCodeSequenceRepository).save(any(com.bemo.hr.employee.domain.EmployeeCodeSequence.class));
    }

    @Test
    void createWithBothScopePromotesAnExistingEmployeeCanonicalCategoryToBoth() {
        var existing = canonical("SECURITY", CategoryScope.EMPLOYEE);
        when(attendanceCategoryRepository.findByCodeIgnoreCase("SECURITY")).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByCategoryId(existing.getId())).thenReturn(false);
        when(categoryRepository.save(any(WorkerCategory.class)))
                .thenAnswer(invocation -> persisted(invocation.getArgument(0)));

        var response = service.create(new WorkforceApi.CategoryRequest("security", "الأمن", null,
                new BigDecimal("220.00"), new BigDecimal("8"), "MONTHLY", "ACTIVE", "BOTH"));

        assertThat(response.id()).isEqualTo(existing.getId());
        assertThat(response.scope()).isEqualTo("BOTH");
        assertThat(existing.getScope()).isEqualTo(CategoryScope.BOTH);
        assertThat(existing.isActive()).isTrue();
        verify(employeeCodeSequenceRepository, never()).save(any());
    }

    @Test
    void createRejectsAWorkerCodeThatAlreadyHasAWorkerConfigLinked() {
        var existing = canonical("WELD", CategoryScope.WORKER);
        when(attendanceCategoryRepository.findByCodeIgnoreCase("WELD")).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByCategoryId(existing.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.create(new WorkforceApi.CategoryRequest("weld", "لحام", null,
                new BigDecimal("275.50"), new BigDecimal("8"), "HALF_MONTH", "ACTIVE", "WORKER")))
                .isInstanceOf(BusinessRuleException.class);
        verify(categoryRepository, never()).save(any(WorkerCategory.class));
    }

    @Test
    void listReturnsOnlyLinkedWorkerCategoriesAndResolvesCanonicalScope() {
        var welder = canonical("WELD", CategoryScope.WORKER);
        var shared = canonical("SECURITY", CategoryScope.BOTH);
        var employeeOnly = canonical("ADMIN", CategoryScope.EMPLOYEE);
        when(attendanceCategoryRepository.findByScopeIn(List.of(CategoryScope.WORKER, CategoryScope.BOTH)))
                .thenReturn(List.of(welder, shared));
        var welderConfig = config(welder);
        var sharedConfig = config(shared);
        when(categoryRepository.findByCategoryIdIn(any())).thenReturn(List.of(welderConfig, sharedConfig));

        var categories = service.list();

        assertThat(categories).hasSize(2);
        assertThat(categories).extracting(WorkforceApi.CategoryResponse::id)
                .containsExactlyInAnyOrder(welder.getId(), shared.getId());
        assertThat(categories).extracting(WorkforceApi.CategoryResponse::scope)
                .containsExactlyInAnyOrder("WORKER", "BOTH");
        assertThat(categories).noneMatch(response -> response.id().equals(employeeOnly.getId()));
    }

    @Test
    void listSkipsWorkerConfigsWhoseCanonicalCategoryNoLongerExists() {
        var welder = canonical("WELD", CategoryScope.WORKER);
        when(attendanceCategoryRepository.findByScopeIn(List.of(CategoryScope.WORKER, CategoryScope.BOTH)))
                .thenReturn(List.of(welder));
        var orphanConfig = config(welder);
        when(categoryRepository.findByCategoryIdIn(any())).thenReturn(List.of(orphanConfig, config(canonical("GHOST", CategoryScope.WORKER))));

        var categories = service.list();

        assertThat(categories).extracting(WorkforceApi.CategoryResponse::id).containsExactly(welder.getId());
    }
}
