package com.bemo.hr.payroll.infrastructure;

import com.bemo.hr.payroll.domain.PayrollRunHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PayrollRunHeaderRepository extends JpaRepository<PayrollRunHeader, String> {
    List<PayrollRunHeader> findByPeriodId(String periodId);
}
