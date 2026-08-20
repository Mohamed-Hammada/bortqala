package com.bemo.hr.project.infrastructure;

import com.bemo.hr.project.domain.WbsNode;
import com.bemo.hr.project.domain.WbsNodeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface WbsNodeRepository extends JpaRepository<WbsNode, String> {

    List<WbsNode> findByProjectId(String projectId);

    List<WbsNode> findByProjectIdOrderBySortOrderAsc(String projectId);

    List<WbsNode> findByProjectIdAndParentIdOrderBySortOrderAsc(String projectId, String parentId);

    List<WbsNode> findByProjectIdAndParentIdIsNullOrderBySortOrderAsc(String projectId);

    boolean existsByProjectIdAndWbsCode(String projectId, String wbsCode);

    boolean existsByProjectIdAndWbsCodeAndIdNot(String projectId, String wbsCode, String id);

    boolean existsByProjectIdAndParentId(String projectId, String parentId);

    boolean existsByProjectIdAndStatus(String projectId, WbsNodeStatus status);

    void deleteByProjectId(String projectId);

    @Query("SELECT w.projectId, COALESCE(SUM(w.plannedAmount), 0), COUNT(w) FROM WbsNode w WHERE w.projectId IN :projectIds GROUP BY w.projectId")
    List<Object[]> summarizeWbsByProjectIds(@Param("projectIds") List<String> projectIds);

    @Query("SELECT COALESCE(SUM(w.plannedAmount), 0) FROM WbsNode w")
    BigDecimal sumTotalPlannedAmount();
}
