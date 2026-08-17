package com.bemo.hr.employee.application;

import com.bemo.hr.employee.domain.AttendanceCategory;
import com.bemo.hr.employee.domain.AttendanceMode;
import com.bemo.hr.employee.domain.PayCycle;
import com.bemo.hr.employee.domain.ScheduleRule;
import com.bemo.hr.employee.infrastructure.AttendanceCategoryRepository;
import com.bemo.hr.employee.infrastructure.ScheduleRuleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@Slf4j
@Profile({"dev", "demo"})
public class DemoReferenceDataService {
    private static final int SATURDAY_TO_THURSDAY_MASK = 111;
    private static final LocalDate DEFAULT_EFFECTIVE_FROM = LocalDate.of(2026, 1, 1);

    private static final List<ReferenceCategory> REFERENCE_CATEGORIES = List.of(
            new ReferenceCategory("SECURITY", "الأمن", 720, PayCycle.THIRTY_DAYS, AttendanceMode.BIOMETRIC, true, false),
            new ReferenceCategory("ACCOUNTING", "الحسابات", 600, PayCycle.THIRTY_DAYS, AttendanceMode.BIOMETRIC, false, true),
            new ReferenceCategory("ADMINISTRATION", "الإداريون", 480, PayCycle.THIRTY_DAYS, AttendanceMode.BIOMETRIC, false, true),
            new ReferenceCategory("SECRETARIAL", "السكرتارية", 480, PayCycle.THIRTY_DAYS, AttendanceMode.BIOMETRIC, false, false),
            new ReferenceCategory("DAILY_WORKERS", "العمالة اليومية", 480, PayCycle.HALF_MONTHLY, AttendanceMode.MANUAL, true, false),
            new ReferenceCategory("CLEANING_WORKERS", "عمال النظافة", 480, PayCycle.HALF_MONTHLY, AttendanceMode.MANUAL, true, false),
            new ReferenceCategory("OPERATION_WORKERS", "عمال التشغيل", 480, PayCycle.HALF_MONTHLY, AttendanceMode.MANUAL, true, false),
            new ReferenceCategory("EXPORT_WORKERS", "عمال التصدير", 480, PayCycle.HALF_MONTHLY, AttendanceMode.MANUAL, true, false),
            new ReferenceCategory("SORTING_WORKERS", "عمال الفرزة", 480, PayCycle.HALF_MONTHLY, AttendanceMode.MANUAL, true, false)
    );

    private final AttendanceCategoryRepository attendanceCategoryRepository;
    private final ScheduleRuleRepository scheduleRuleRepository;

    public DemoReferenceDataService(AttendanceCategoryRepository attendanceCategoryRepository,
                                    ScheduleRuleRepository scheduleRuleRepository) {
        this.attendanceCategoryRepository = attendanceCategoryRepository;
        this.scheduleRuleRepository = scheduleRuleRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ensureReferenceConfiguration() {
        REFERENCE_CATEGORIES.stream()
                .filter(reference -> !attendanceCategoryRepository.existsByCodeIgnoreCase(reference.code()))
                .forEach(this::createReferenceCategory);
    }

    private void createReferenceCategory(ReferenceCategory reference) {
        var category = attendanceCategoryRepository.save(new AttendanceCategory(
                reference.code(), reference.name(), reference.expectedMinutes(), reference.payCycle(),
                reference.attendanceMode(), reference.singlePunchCounts(), SATURDAY_TO_THURSDAY_MASK, true));
        category.configureAdvanceEligibility(reference.allowsAdvances());
        scheduleRuleRepository.saveAll(List.of(
                new ScheduleRule(category.getId(), "الدوام الشتوي", DEFAULT_EFFECTIVE_FROM,
                        LocalDate.of(2026, 4, 30), LocalTime.of(9, 0), null, 0),
                new ScheduleRule(category.getId(), "الدوام الصيفي", LocalDate.of(2026, 5, 1),
                        LocalDate.of(2026, 9, 30), LocalTime.of(8, 0), null, 0),
                new ScheduleRule(category.getId(), "الدوام الشتوي", LocalDate.of(2026, 10, 1),
                        null, LocalTime.of(9, 0), null, 0)
        ));
    }

    private record ReferenceCategory(String code, String name, int expectedMinutes, PayCycle payCycle,
                                     AttendanceMode attendanceMode, boolean singlePunchCounts, boolean allowsAdvances) {
    }
}
