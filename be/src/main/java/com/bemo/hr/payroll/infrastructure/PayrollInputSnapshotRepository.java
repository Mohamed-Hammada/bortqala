package com.bemo.hr.payroll.infrastructure;

import com.bemo.hr.payroll.domain.PayrollInputSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollInputSnapshotRepository extends JpaRepository<PayrollInputSnapshot, String> {
    Optional<PayrollInputSnapshot> findByPayrollRunIdAndEmployeeId(String payrollRunId, String employeeId);

    List<PayrollInputSnapshot> findByPayrollRunId(String payrollRunId);
}
