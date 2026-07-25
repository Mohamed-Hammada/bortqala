package com.bemo.hr.employee.application;

import com.bemo.hr.employee.domain.AttendanceCategory;
import com.bemo.hr.employee.infrastructure.AttendanceCategoryRepository;
import com.bemo.hr.employee.infrastructure.ScheduleRuleRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DemoReferenceDataServiceTests {
    @Test
    void createsAllMissingReferenceCategoriesAndSchedules() {
        var categoryRepository = mock(AttendanceCategoryRepository.class);
        var scheduleRepository = mock(ScheduleRuleRepository.class);
        when(categoryRepository.existsByCodeIgnoreCase(any())).thenReturn(false);
        when(categoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var service = new DemoReferenceDataService(categoryRepository, scheduleRepository);

        service.ensureReferenceConfiguration();

        var categories = ArgumentCaptor.forClass(AttendanceCategory.class);
        verify(categoryRepository, times(9)).save(categories.capture());
        verify(scheduleRepository, times(9)).saveAll(any());
        assertThat(categories.getAllValues())
                .anyMatch(category -> category.getCode().equals("ACCOUNTING")
                        && category.getExpectedDailyMinutes() == 600)
                .anyMatch(category -> category.getCode().equals("SECRETARIAL"))
                .anyMatch(category -> category.getCode().equals("DAILY_WORKERS")
                        && category.isSinglePunchCounts());
    }
}
