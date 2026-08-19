package com.bemo.hr.project.infrastructure;

import com.bemo.hr.project.domain.ProgressClaimAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProgressClaimAdjustmentRepository extends JpaRepository<ProgressClaimAdjustment, String> {

    List<ProgressClaimAdjustment> findByClaimId(String claimId);

    void deleteByClaimId(String claimId);
}
