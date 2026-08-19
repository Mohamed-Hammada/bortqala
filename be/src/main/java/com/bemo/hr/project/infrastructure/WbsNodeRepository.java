package com.bemo.hr.project.infrastructure;

import com.bemo.hr.project.domain.WbsNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WbsNodeRepository extends JpaRepository<WbsNode, String> {

    List<WbsNode> findByProjectIdOrderBySortOrderAsc(String projectId);

    List<WbsNode> findByProjectIdAndParentIdOrderBySortOrderAsc(String projectId, String parentId);

    List<WbsNode> findByProjectIdAndParentIdIsNullOrderBySortOrderAsc(String projectId);

    boolean existsByProjectIdAndWbsCode(String projectId, String wbsCode);

    boolean existsByProjectIdAndWbsCodeAndIdNot(String projectId, String wbsCode, String id);

    boolean existsByProjectIdAndParentId(String projectId, String parentId);

    void deleteByProjectId(String projectId);
}
