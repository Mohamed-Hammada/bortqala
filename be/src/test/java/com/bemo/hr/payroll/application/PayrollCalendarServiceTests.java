package com.bemo.hr.payroll.application;

import com.bemo.hr.payroll.domain.PayPeriod;
import com.bemo.hr.payroll.domain.PayrollCalendar;
import com.bemo.hr.payroll.infrastructure.PayPeriodRepository;
import com.bemo.hr.payroll.infrastructure.PayrollCalendarRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PayrollCalendarServiceTests {

    private PayrollCalendarRepository calendarRepository;
    private PayPeriodRepository periodRepository;
    private PayrollCalendarService calendarService;

    @BeforeEach
    void setUp() {
        calendarRepository = mock(PayrollCalendarRepository.class);
        periodRepository = mock(PayPeriodRepository.class);
        calendarService = new PayrollCalendarService(calendarRepository, periodRepository);
    }

    @Test
    void createsCalendarGeneratesPeriodsAndClosesPeriodSuccessfully() {
        when(calendarRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(periodRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PayrollCalendar cal = calendarService.createCalendar("CAL-MONTHLY", "Monthly Staff Payroll", PayrollCalendar.Frequency.MONTHLY);
        assertThat(cal).isNotNull();
        assertThat(cal.getCalendarCode()).isEqualTo("CAL-MONTHLY");

        when(calendarRepository.findById(cal.getId())).thenReturn(Optional.of(cal));
        List<PayPeriod> periods = calendarService.generatePeriods(cal.getId(), 2026);
        assertThat(periods).hasSize(12);

        PayPeriod period = periods.get(0);
        when(periodRepository.findById(period.getId())).thenReturn(Optional.of(period));
        calendarService.closePeriod(period.getId());
        assertThat(period.getStatus()).isEqualTo(PayPeriod.Status.CLOSED);
    }
}
