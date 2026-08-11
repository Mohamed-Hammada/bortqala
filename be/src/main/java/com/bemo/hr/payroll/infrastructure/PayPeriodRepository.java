package com.bemo.hr.payroll.infrastructure;

import com.bemo.hr.payroll.domain.PayPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PayPeriodRepository extends JpaRepository<PayPeriod, String> {
    List<PayPeriod> findByCalendarId(String calendarId);
    List<PayPeriod> findByCalendarIdAndStatus(String calendarId, PayPeriod.Status status);
}
