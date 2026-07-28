package com.bemo.hr.workforce;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WorkforceAdvanceInstallmentRepository extends JpaRepository<WorkforceAdvanceInstallment, String> {
    List<WorkforceAdvanceInstallment> findByAdvanceId(String advanceId);
    List<WorkforceAdvanceInstallment> findByStatus(String status);
}
