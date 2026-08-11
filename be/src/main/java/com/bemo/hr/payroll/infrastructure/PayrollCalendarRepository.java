package com.bemo.hr.payroll.infrastructure;

import com.bemo.hr.payroll.domain.PayrollCalendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PayrollCalendarRepository extends JpaRepository<PayrollCalendar, String> {
    Optional<PayrollCalendar> findByCalendarCode(String calendarCode);
}
