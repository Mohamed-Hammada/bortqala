package com.bemo.hr.payroll.application;

import com.bemo.hr.payroll.domain.PayPeriod;
import com.bemo.hr.payroll.domain.PayrollCalendar;
import com.bemo.hr.payroll.infrastructure.PayPeriodRepository;
import com.bemo.hr.payroll.infrastructure.PayrollCalendarRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Slf4j
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
        log.debug("createCalendar called with calendarCode={}, name={}, frequency={}", calendarCode, name, frequency);
        PayrollCalendar calendar = new PayrollCalendar(calendarCode, name, frequency);
        PayrollCalendar saved = calendarRepository.save(calendar);
        log.info("PayrollCalendar created id={}", saved.getId());
        return saved;
    }

    @Transactional
    public List<PayPeriod> generatePeriods(String calendarId, int year) {
        log.debug("generatePeriods called with calendarId={}, year={}", calendarId, year);
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
        log.info("PayrollCalendar generated {} periods for calendarId={} year={}", periods.size(), calendarId, year);
        return periods;
    }

    @Transactional
    public PayPeriod closePeriod(String periodId) {
        log.debug("closePeriod called with periodId={}", periodId);
        PayPeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new BusinessRuleException("Pay period not found", "PAY_PERIOD_NOT_FOUND", HttpStatus.NOT_FOUND));
        period.close();
        PayPeriod saved = periodRepository.save(period);
        log.info("PayPeriod closed id={}", saved.getId());
        return saved;
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
