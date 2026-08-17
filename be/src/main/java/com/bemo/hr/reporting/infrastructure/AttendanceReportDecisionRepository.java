package com.bemo.hr.reporting.infrastructure;

import com.bemo.hr.reporting.domain.AttendanceReportDecision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttendanceReportDecisionRepository extends JpaRepository<AttendanceReportDecision, String> {
    List<AttendanceReportDecision> findByResultIdOrderByCreatedAtAsc(String resultId);

    List<AttendanceReportDecision> findByReportIdOrderByCreatedAtAsc(String reportId);

    long countByReportId(String reportId);
}
