package com.bemo.hr.payroll.infrastructure;

import com.bemo.hr.payroll.domain.PayrollRunLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PayrollRunLineRepository extends JpaRepository<PayrollRunLine, String> {
    List<PayrollRunLine> findByRunId(String runId);
}
