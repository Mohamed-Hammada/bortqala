package com.bemo.hr.manufacturing.production.infrastructure;

import com.bemo.hr.manufacturing.production.domain.MaterialIssueLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaterialIssueLineRepository extends JpaRepository<MaterialIssueLine, String> {
    List<MaterialIssueLine> findByIssueId(String issueId);
}
