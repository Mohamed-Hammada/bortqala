package com.bemo.hr.payroll.infrastructure;

import com.bemo.hr.payroll.domain.PayrollCalculationPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PayrollCalculationPolicyRepository extends JpaRepository<PayrollCalculationPolicy, String> {
    List<PayrollCalculationPolicy> findByActiveTrueOrderByEffectiveFromDesc();
}
