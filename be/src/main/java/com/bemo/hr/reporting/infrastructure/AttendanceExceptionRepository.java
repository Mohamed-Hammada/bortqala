package com.bemo.hr.reporting.infrastructure;

import com.bemo.hr.reporting.domain.AttendanceException;
import com.bemo.hr.reporting.domain.AttendanceExceptionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AttendanceExceptionRepository extends JpaRepository<AttendanceException, String> {
    List<AttendanceException> findByReportIdOrderByScoreDescWorkDateAsc(String reportId);

    boolean existsByReportIdAndDailyResultIdAndExceptionType(String reportId, String resultId, com.bemo.hr.reporting.domain.AttendanceExceptionType type);

    long countByReportIdAndEmployeeIdAndStatusAndPayrollBlockingTrue(String reportId, String employeeId, AttendanceExceptionStatus status);

    long countByReportIdAndStatusAndPayrollBlockingTrue(String reportId, AttendanceExceptionStatus status);

    @Modifying
    @Query("delete from AttendanceException e where e.reportId = :reportId")
    void deleteByReportId(@Param("reportId") String reportId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from AttendanceException e where e.id in :ids")
    List<AttendanceException> findAllByIdForUpdate(@Param("ids") List<String> ids);
}
