package com.bemo.hr.finance.infrastructure;

import com.bemo.hr.finance.domain.CostCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CostCenterRepository extends JpaRepository<CostCenter, String> {
    List<CostCenter> findAllByOrderByCodeAsc();
    List<CostCenter> findByActiveTrueOrderByCodeAsc();
    Optional<CostCenter> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByParentId(String parentId);
}
