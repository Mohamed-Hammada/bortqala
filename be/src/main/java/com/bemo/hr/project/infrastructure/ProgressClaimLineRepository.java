package com.bemo.hr.project.infrastructure;

import com.bemo.hr.project.domain.ProgressClaimLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProgressClaimLineRepository extends JpaRepository<ProgressClaimLine, String> {

    List<ProgressClaimLine> findByClaimIdOrderBySortOrderAsc(String claimId);

    void deleteByClaimId(String claimId);
}
