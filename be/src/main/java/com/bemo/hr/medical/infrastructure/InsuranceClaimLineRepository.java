package com.bemo.hr.medical.infrastructure;

import com.bemo.hr.medical.domain.InsuranceClaimLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InsuranceClaimLineRepository extends JpaRepository<InsuranceClaimLine, String> {

    Optional<InsuranceClaimLine> findByAppIdAndId(String appId, String id);

    List<InsuranceClaimLine> findAllByAppIdAndBatchIdOrderByCreatedAtAsc(String appId, String batchId);

    List<InsuranceClaimLine> findAllByAppIdAndVisitId(String appId, String visitId);
}
