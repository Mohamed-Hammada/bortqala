package com.bemo.hr.manufacturing.production.infrastructure;

import com.bemo.hr.manufacturing.production.domain.MaterialIssueHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaterialIssueHeaderRepository extends JpaRepository<MaterialIssueHeader, String> {
    List<MaterialIssueHeader> findByProductionOrderId(String productionOrderId);
}
