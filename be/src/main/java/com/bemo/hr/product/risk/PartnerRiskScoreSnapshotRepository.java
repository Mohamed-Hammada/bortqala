package com.bemo.hr.product.risk;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartnerRiskScoreSnapshotRepository extends JpaRepository<PartnerRiskScoreSnapshot, String> {
    List<PartnerRiskScoreSnapshot> findByOperationIdOrderBySubjectTypeAscSubjectNameAsc(String operationId);
}
