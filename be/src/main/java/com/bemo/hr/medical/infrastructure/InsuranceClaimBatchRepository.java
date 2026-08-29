package com.bemo.hr.medical.infrastructure;

import com.bemo.hr.medical.domain.InsuranceClaimBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InsuranceClaimBatchRepository extends JpaRepository<InsuranceClaimBatch, String> {

    Optional<InsuranceClaimBatch> findByAppIdAndId(String appId, String id);

    Optional<InsuranceClaimBatch> findByAppIdAndBatchNumber(String appId, String batchNumber);

    List<InsuranceClaimBatch> findAllByAppIdOrderByCreatedAtDesc(String appId);

    List<InsuranceClaimBatch> findAllByAppIdAndPayerIdOrderByCreatedAtDesc(String appId, String payerId);
}
