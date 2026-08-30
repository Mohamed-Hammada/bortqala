package com.bemo.hr.project.infrastructure;

import com.bemo.hr.project.domain.CostCodeCategory;
import com.bemo.hr.project.domain.ProjectCostCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectCostCodeRepository extends JpaRepository<ProjectCostCode, String> {

    Optional<ProjectCostCode> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, String id);

    List<ProjectCostCode> findAllByOrderByCodeAsc();

    List<ProjectCostCode> findByActiveTrueOrderByCodeAsc();

    List<ProjectCostCode> findByCategoryOrderByCodeAsc(CostCodeCategory category);
}
