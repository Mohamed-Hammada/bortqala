package com.bemo.hr.project.infrastructure;

import com.bemo.hr.project.domain.CostCategory;
import com.bemo.hr.project.domain.CostLedgerEntryType;
import com.bemo.hr.project.domain.ProjectCostLedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProjectCostLedgerEntryRepository extends JpaRepository<ProjectCostLedgerEntry, String> {

    List<ProjectCostLedgerEntry> findByProjectIdOrderByEntryDateDesc(String projectId);

    List<ProjectCostLedgerEntry> findByProjectIdAndEntryTypeOrderByEntryDateDesc(String projectId, CostLedgerEntryType entryType);

    List<ProjectCostLedgerEntry> findByProjectIdAndWbsNodeId(String projectId, String wbsNodeId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM ProjectCostLedgerEntry e WHERE e.projectId = :projectId AND e.entryType = :entryType")
    BigDecimal sumAmountByProjectIdAndEntryType(@Param("projectId") String projectId, @Param("entryType") CostLedgerEntryType entryType);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM ProjectCostLedgerEntry e WHERE e.projectId = :projectId AND e.costCategory = :cat AND e.entryType = :entryType")
    BigDecimal sumAmountByProjectIdAndCategoryAndEntryType(
            @Param("projectId") String projectId,
            @Param("cat") CostCategory cat,
            @Param("entryType") CostLedgerEntryType entryType
    );
}
