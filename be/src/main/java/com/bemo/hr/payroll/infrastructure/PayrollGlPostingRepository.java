package com.bemo.hr.payroll.infrastructure;

import com.bemo.hr.payroll.domain.PayrollGlPosting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PayrollGlPostingRepository extends JpaRepository<PayrollGlPosting, String> {
    Optional<PayrollGlPosting> findByPayrollPeriodId(String payrollPeriodId);
}
