package com.bemo.hr.reporting.scheduled.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportScheduleRepository extends JpaRepository<ReportSchedule, String> {
    List<ReportSchedule> findByAppIdOrderByCreatedAtDesc(String appId);
    List<ReportSchedule> findByAppIdAndActiveTrue(String appId);
}
