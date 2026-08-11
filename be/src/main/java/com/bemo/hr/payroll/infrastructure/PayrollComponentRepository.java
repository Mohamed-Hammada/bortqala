package com.bemo.hr.payroll.infrastructure;

import com.bemo.hr.payroll.domain.PayrollComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PayrollComponentRepository extends JpaRepository<PayrollComponent, String> {
    Optional<PayrollComponent> findByCode(String code);
}
