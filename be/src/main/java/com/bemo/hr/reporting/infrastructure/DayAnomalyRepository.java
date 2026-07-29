package com.bemo.hr.reporting.infrastructure;

import com.bemo.hr.reporting.domain.DayAnomaly;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DayAnomalyRepository extends JpaRepository<DayAnomaly, String> {
    List<DayAnomaly> findByReportIdOrderByWorkDateAscCategoryNameAsc(String reportId);
    Optional<DayAnomaly> findByReportIdAndCategoryIdAndWorkDate(String reportId, String categoryId, LocalDate workDate);
}
