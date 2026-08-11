package com.bemo.hr.payroll.application;

import com.bemo.hr.payroll.domain.PayPeriod;
import com.bemo.hr.payroll.domain.PayrollCalendar;
import com.bemo.hr.payroll.infrastructure.PayPeriodRepository;
import com.bemo.hr.payroll.infrastructure.PayrollCalendarRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
public class PayrollCalendarService {

    private final PayrollCalendarRepository calendarRepository;
    private final PayPeriodRepository periodRepository;

    public PayrollCalendarService(PayrollCalendarRepository calendarRepository,
                                  PayPeriodRepository periodRepository) {
        this.calendarRepository = calendarRepository;
        this.periodRepository = periodRepository;
    }

    @Transactional
    public PayrollCalendar createCalendar(String calendarCode, String name, PayrollCalendar.Frequency frequency) {
        PayrollCalendar calendar = new PayrollCalendar(calendarCode, name, frequency);
        return calendarRepository.save(calendar);
    }

    @Transactional
    public List<PayPeriod> generatePeriods(String calendarId, int year) {
        PayrollCalendar calendar = calendarRepository.findById(calendarId)
                .orElseThrow(() -> new BusinessRuleException("Payroll calendar not found", "CALENDAR_NOT_FOUND", HttpStatus.NOT_FOUND));

        List<PayPeriod> periods = new ArrayList<>();
        if (calendar.getFrequency() == PayrollCalendar.Frequency.MONTHLY) {
            for (int month = 1; month <= 12; month++) {
                YearMonth ym = YearMonth.of(year, month);
                LocalDate start = ym.atDay(1);
                LocalDate end = ym.atEndOfMonth();
                PayPeriod period = new PayPeriod(calendarId, month, start, end);
                periods.add(periodRepository.save(period));
            }
        }
        return periods;
    }

    @Transactional
    public PayPeriod closePeriod(String periodId) {
        PayPeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new BusinessRuleException("Pay period not found", "PAY_PERIOD_NOT_FOUND", HttpStatus.NOT_FOUND));
        period.close();
        return periodRepository.save(period);
    }

    @Transactional(readOnly = true)
    public List<PayrollCalendar> getAllCalendars() {
        return calendarRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<PayPeriod> getPeriodsByCalendar(String calendarId) {
        return periodRepository.findByCalendarId(calendarId);
    }
}
