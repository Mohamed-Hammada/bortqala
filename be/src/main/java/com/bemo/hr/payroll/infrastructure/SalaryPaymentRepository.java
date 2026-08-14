package com.bemo.hr.payroll.infrastructure;

import com.bemo.hr.payroll.domain.SalaryPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SalaryPaymentRepository extends JpaRepository<SalaryPayment, String> {
    List<SalaryPayment> findByPeriodYearAndPeriodMonthOrderByCreatedAtDesc(int periodYear, int periodMonth);
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
