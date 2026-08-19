package com.bemo.hr.payroll.api;

import com.bemo.hr.payroll.application.PayrollCalendarService;
import com.bemo.hr.payroll.domain.PayPeriod;
import com.bemo.hr.payroll.domain.PayrollCalendar;
import com.bemo.hr.shared.security.Roles;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payroll")
public class PayrollCalendarController {

    private final PayrollCalendarService calendarService;

    public PayrollCalendarController(PayrollCalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @PostMapping("/calendars")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.HR_MANAGER + " or " + Roles.PAYROLL_MANAGER)
    public PayrollCalendar createCalendar(@RequestBody CreateCalendarPayload payload) {
        return calendarService.createCalendar(payload.calendarCode(), payload.name(), PayrollCalendar.Frequency.valueOf(payload.frequency()));
    }

    @PostMapping("/calendars/{id}/generate-periods")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.HR_MANAGER + " or " + Roles.PAYROLL_MANAGER)
    public List<PayPeriod> generatePeriods(@PathVariable String id, @RequestBody GeneratePeriodsPayload payload) {
        return calendarService.generatePeriods(id, payload.year());
    }

    @PostMapping("/periods/{id}/close")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.HR_MANAGER + " or " + Roles.PAYROLL_MANAGER)
    public PayPeriod closePeriod(@PathVariable String id) {
        return calendarService.closePeriod(id);
    }

    @GetMapping("/calendars")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.HR_MANAGER + " or " + Roles.PAYROLL_MANAGER + " or " + Roles.VIEWER)
    public List<PayrollCalendar> getAllCalendars() {
        return calendarService.getAllCalendars();
    }

    @GetMapping("/calendars/{id}/periods")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.HR_MANAGER + " or " + Roles.PAYROLL_MANAGER + " or " + Roles.VIEWER)
    public List<PayPeriod> getPeriodsByCalendar(@PathVariable String id) {
        return calendarService.getPeriodsByCalendar(id);
    }

    public record CreateCalendarPayload(String calendarCode, String name, String frequency) {
    }

    public record GeneratePeriodsPayload(int year) {
    }
}
