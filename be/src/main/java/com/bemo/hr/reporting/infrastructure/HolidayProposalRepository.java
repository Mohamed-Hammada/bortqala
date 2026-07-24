package com.bemo.hr.reporting.infrastructure;

import com.bemo.hr.reporting.domain.HolidayProposal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HolidayProposalRepository extends JpaRepository<HolidayProposal, String> {
    List<HolidayProposal> findByReportIdOrderByWorkDateAscCategoryNameAsc(String reportId);
}
