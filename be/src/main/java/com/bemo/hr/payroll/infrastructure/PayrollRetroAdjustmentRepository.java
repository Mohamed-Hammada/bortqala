package com.bemo.hr.payroll.infrastructure;

import com.bemo.hr.payroll.domain.PayrollRetroAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PayrollRetroAdjustmentRepository extends JpaRepository<PayrollRetroAdjustment, String> {
    List<PayrollRetroAdjustment> findByEmployeeId(String employeeId);
}
