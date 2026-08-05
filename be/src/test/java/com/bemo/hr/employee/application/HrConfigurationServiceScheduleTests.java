package com.bemo.hr.employee.application;

import com.bemo.hr.employee.api.CategoryApi;
import com.bemo.hr.employee.domain.AttendanceCategory;
import com.bemo.hr.employee.domain.AttendanceMode;
import com.bemo.hr.employee.domain.PayCycle;
import com.bemo.hr.employee.infrastructure.AttendanceCategoryRepository;
import com.bemo.hr.employee.infrastructure.EmployeeAssignmentRepository;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.employee.infrastructure.EmployeeCodeSequenceRepository;
import com.bemo.hr.employee.infrastructure.ScheduleRuleRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.AppUserRepository;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        var service = new HrConfigurationService(categoryRepository, scheduleRepository, employeeRepository,
                mock(EmployeeCodeSequenceRepository.class), mock(EmployeeAssignmentRepository.class),
                mock(AppUserRepository.class));
        var schedule = new CategoryApi.ScheduleRequest("الجدول الأساسي", LocalDate.of(2026, 1, 1),
                null, LocalTime.of(8, 0), null, 0);
        var request = new CategoryApi.UpsertRequest("SECURITY", "الأمن", 720, PayCycle.THIRTY_DAYS,
                AttendanceMode.BIOMETRIC, false, false, Set.of(DayOfWeek.SATURDAY), true, List.of(schedule), 0L);

        service.updateCategory(category.getId(), request);

        var order = inOrder(scheduleRepository);
        order.verify(scheduleRepository).deleteByCategoryId(category.getId());
        order.verify(scheduleRepository).flush();
        order.verify(scheduleRepository).saveAll(org.mockito.ArgumentMatchers.any());
    }

    private static CategoryApi.ScheduleRequest schedule(int fromMonth, Integer toMonth) {
        return new CategoryApi.ScheduleRequest("الجدول", LocalDate.of(2026, fromMonth, 1),
                toMonth == null ? null : LocalDate.of(2026, toMonth, 28),
                LocalTime.of(8, 0), null, 0);
    }

    @Test
    void rejectsNonAdjacentOverlapWithScheduleRuleOverlap422() {
        var overlaps = List.of(schedule(1, 1), schedule(2, 2), schedule(1, 1));
        assertThatThrownBy(() -> HrConfigurationService.validateScheduleRanges(overlaps))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(exception -> {
                    var business = (BusinessRuleException) exception;
                    assertThat(business.getCode()).isEqualTo("SCHEDULE_RULE_OVERLAP");
                    assertThat(business.getStatus().value()).isEqualTo(422);
                    assertThat(business.getFields()).containsExactly("schedules[0]", "schedules[2]");
                });
    }

    @Test
    void allowsAdjacentRangesWithSameBoundary() {
        var adjacent = List.of(schedule(1, 1), schedule(2, null));
        assertThatCode(() -> HrConfigurationService.validateScheduleRanges(adjacent)).doesNotThrowAnyException();
    }

    @Test
    void rejectsOpenEndedRangeOverlappingALaterSchedule() {
        var openEnded = List.of(schedule(1, null), schedule(2, 2));
        assertThatThrownBy(() -> HrConfigurationService.validateScheduleRanges(openEnded))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(exception ->
                        assertThat(((BusinessRuleException) exception).getCode()).isEqualTo("SCHEDULE_RULE_OVERLAP"));
    }

    @Test
    void rejectsEndDateBeforeStartDate() {
        var invalid = List.of(
                new CategoryApi.ScheduleRequest("الجدول", LocalDate.of(2026, 2, 10),
                        LocalDate.of(2026, 2, 5), LocalTime.of(8, 0), null, 0));
        assertThatThrownBy(() -> HrConfigurationService.validateScheduleRanges(invalid))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("before its start date");
    }
}
