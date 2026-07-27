package com.bemo.hr.payroll.infrastructure;

import com.bemo.hr.payroll.domain.SalaryPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SalaryPaymentRepository extends JpaRepository<SalaryPayment, String> {
    List<SalaryPayment> findByPeriodYearAndPeriodMonthOrderByCreatedAtDesc(int periodYear, int periodMonth);
    Optional<SalaryPayment> findByEmployeeIdAndPeriodYearAndPeriodMonthAndPeriodKind(
            String employeeId, int periodYear, int periodMonth, String periodKind);
    List<SalaryPayment> findByEmployeeIdOrderByPeriodStartDesc(String employeeId);
}
