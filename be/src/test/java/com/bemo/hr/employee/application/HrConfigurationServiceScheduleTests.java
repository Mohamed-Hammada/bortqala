package com.bemo.hr.employee.application;

import com.bemo.hr.employee.api.CategoryApi;
import com.bemo.hr.employee.domain.AttendanceCategory;
import com.bemo.hr.employee.domain.AttendanceMode;
import com.bemo.hr.employee.domain.PayCycle;
import com.bemo.hr.employee.infrastructure.AttendanceCategoryRepository;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.employee.infrastructure.ScheduleRuleRepository;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HrConfigurationServiceScheduleTests {
    @Test
    void flushesDeletedSchedulesBeforeInsertingReplacements() {
        var categoryRepository = mock(AttendanceCategoryRepository.class);
        var scheduleRepository = mock(ScheduleRuleRepository.class);
        var employeeRepository = mock(EmployeeRepository.class);
        var category = new AttendanceCategory("SECURITY", "الأمن", 480, PayCycle.MONTHLY,
                AttendanceMode.BIOMETRIC, false, 111, true);
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(scheduleRepository.findByCategoryIdOrderByEffectiveFromAsc(category.getId())).thenReturn(List.of());
        var service = new HrConfigurationService(categoryRepository, scheduleRepository, employeeRepository);
        var schedule = new CategoryApi.ScheduleRequest("الجدول الأساسي", LocalDate.of(2026, 1, 1),
                null, LocalTime.of(8, 0), null, 0);
        var request = new CategoryApi.UpsertRequest("SECURITY", "الأمن", 720, PayCycle.THIRTY_DAYS,
                AttendanceMode.BIOMETRIC, false, Set.of(DayOfWeek.SATURDAY), true, List.of(schedule), 0L);

        service.updateCategory(category.getId(), request);

        var order = inOrder(scheduleRepository);
        order.verify(scheduleRepository).deleteByCategoryId(category.getId());
        order.verify(scheduleRepository).flush();
        order.verify(scheduleRepository).saveAll(org.mockito.ArgumentMatchers.any());
    }
}
