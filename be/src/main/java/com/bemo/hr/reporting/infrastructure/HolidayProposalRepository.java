package com.bemo.hr.reporting.infrastructure;

import com.bemo.hr.reporting.domain.HolidayProposal;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HolidayProposalRepository extends JpaRepository<HolidayProposal, String> {
    List<HolidayProposal> findByReportIdOrderByWorkDateAscCategoryNameAsc(String reportId);

    @Modifying
    @Query("delete from HolidayProposal h where h.reportId = :reportId")
    void deleteByReportId(@Param("reportId") String reportId);
}
