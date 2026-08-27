package com.bemo.hr.reportbuilder.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SavedReportRepository extends JpaRepository<SavedReport, String> {
    List<SavedReport> findByAppIdOrderByCreatedAtDesc(String appId);
}
