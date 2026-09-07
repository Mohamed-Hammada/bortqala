package com.bemo.hr.payroll.infrastructure;

import com.bemo.hr.payroll.domain.SalaryPayment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SalaryPaymentRepository extends JpaRepository<SalaryPayment, String> {
    List<SalaryPayment> findByPeriodYearAndPeriodMonthOrderByCreatedAtDesc(int periodYear, int periodMonth);

    /**
     * 2026-09-07 remediation (branch-filtering hardening): SalaryPayment has no branchId of its
     * own, but a real join through employeeId -> Employee.branchId exists — used to give the Owner
     * Cockpit real, branch-scoped payroll figures when a specific branch is requested.
     */
    List<SalaryPayment> findByEmployeeIdInAndPeriodYearAndPeriodMonth(Collection<String> employeeIds, int periodYear, int periodMonth);

    List<SalaryPayment> findByPayrollRunId(String payrollRunId);

    Optional<SalaryPayment> findByEmployeeIdAndPeriodYearAndPeriodMonthAndPeriodKind(
            String employeeId, int periodYear, int periodMonth, String periodKind);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from SalaryPayment p where p.employeeId = :employeeId and p.periodYear = :periodYear "
            + "and p.periodMonth = :periodMonth and p.periodKind = :periodKind")
    Optional<SalaryPayment> findForUpdate(@Param("employeeId") String employeeId,
                                          @Param("periodYear") int periodYear,
                                          @Param("periodMonth") int periodMonth,
                                          @Param("periodKind") String periodKind);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from SalaryPayment p where p.id = :id")
    Optional<SalaryPayment> findByIdForUpdate(@Param("id") String id);

    List<SalaryPayment> findByEmployeeIdOrderByPeriodStartDesc(String employeeId);
}
