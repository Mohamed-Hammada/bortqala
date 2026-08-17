package com.bemo.hr.payroll.infrastructure;

import com.bemo.hr.payroll.domain.PayrollRunLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollRunLineRepository extends JpaRepository<PayrollRunLine, String> {
    List<PayrollRunLine> findByRunId(String runId);

    Optional<PayrollRunLine> findByRunIdAndEmployeeId(String runId, String employeeId);
}
